const language = require('@google-cloud/language');
const client = new language.LanguageServiceClient();

const rp = require('request-promise');
const cheerio = require('cheerio');

var options = {
    uri: `https://en.wikipedia.org/wiki/Disodium_guanylate`,
    transform: function (body) {
        return cheerio.load(body);
    }
};

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
    return rp(options).then(($) => {
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
