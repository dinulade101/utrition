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
    return rp({
        uri: 'https://en.wikipedia.org/w/api.php',
        json: true,
        qs: {
            action: 'query',
            format: 'json',
            prop: 'info',
            inprop: 'url',
            titles: ingredient,
        },
    }).then(res => {
        console.log(res);
        var page = res.query.pages[Object.keys(res.query.pages)[0]];
        if (!page) {
            throw 'wiki page not found';
        }
        console.log(page);
        return page.fullurl;
    }).then(url => {
        return rp({
            uri: url,
            transform: function (body) {
                return cheerio.load(body);
            }
        });
    }).then(($) => {
	    return $('p').text().split('\n').filter(f => {
			return re.test(f);
		}).join(' ');
 	}).catch((err) => {
        return err;
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
    descriptions =  Promise.all(ingredients.map(crawl))
    scores = descriptions.then(desc => {
        return score(desc.join());
    });
    return Promise.all([descriptions, scores]).then(([desc, scores]) => {
        return {
            ingredients: ingredients.map((ingredient, i) => {
                return {
                    name: ingredient,
                    description: desc[i],
                    sentiment: scores[0].sentences[i].sentiment
                };
            }),
            sentiment: scores[0].documentSentiment
        };
    });
}
