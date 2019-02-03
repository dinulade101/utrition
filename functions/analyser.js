const language = require('@google-cloud/language');
const client = new language.LanguageServiceClient();

function crawl (ingredient) {
    return 'chemical health concern summary';
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