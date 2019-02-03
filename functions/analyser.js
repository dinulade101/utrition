const language = require('@google-cloud/language');
const client = new language.LanguageServiceClient();

const rp = require('request-promise');
const cheerio = require('cheerio');

const re = (function() {
    var components = [],
        arg;

    for (var i = 0; i < arguments.length; i++) {
        arg = arguments[i];
        if (arg instanceof RegExp) {
            components = components.concat(arg._components || arg.source);
        }
    }

    var combined = new RegExp("(?:" + components.join(")|(?:") + ")");
    combined._components = components; // For chained calls to "or" method
    return combined;
})(...require('./keywords'));


function crawl (ingredient) {
    let page = rp({
        uri: 'https://en.wikipedia.org/w/api.php',
        json: true,
        qs: {
            action: 'query',
            format: 'json',
            prop: 'info|extracts',
            inprop: 'url',
            exintro: 1,
            explaintext: 1,
            titles: ingredient.toLowerCase(),
        },
    }).then(res => {
        //console.log(res);
        var page = res.query.pages[Object.keys(res.query.pages)[0]];
        if (!page) {
            throw 'wiki page not found';
        }
        //console.log(page);
        return {
            url: page.fullurl,
            extract: page.extract
        };
    });

    let desc = page.then(page => {
        return rp({
            uri: page.url,
            transform: function (body) {
                return cheerio.load(body);
            }
        });
    }).then(($) => {
	    return $('p').text().split('\n').filter(f => {
			return re.test(f);
		}).join(' ');
 	}).catch((err) => {
        if (err) {
            return;
        }
    });

    return Promise.all([page, desc]).then(([page, desc]) => {
        return {
            url: page.url,
            description: desc,
            extract: page.extract
        };
    });

}

function score (summary) {
    return client.analyzeSentiment({
        document: {
            content: summary,
            type: 'PLAIN_TEXT'
        },
        encodingType: 'UTF8'
    });
}

module.exports = function (ingredients) {
    descriptions =  Promise.all(ingredients.map(crawl)).then(res => {
        return res.filter(r => !!r);
    });
    scores = descriptions.then(desc => {
        return score(desc.map(d => d.description).join('\n\n'));
    });
    return Promise.all([descriptions, scores]).then(([desc, scores]) => {
        return {
            ingredients: ingredients.map((ingredient, i) => {
                let txt = desc[i].extract || desc[i].description || " ";
                let s = scores[0].sentences[i] || {sentiment: " "};
                console.log(txt);
                return {
                    name: ingredient,
                    description: txt.split(" ").slice(0,20).join(" ") + "...",
                    url: desc[i].url,
                    sentiment: s.sentiment
                };
            }),
            sentiment: scores[0].documentSentiment
        };
    });
}
