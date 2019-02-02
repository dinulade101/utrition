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
    return Promise.all(ingredients.map(crawl)).then(res => {
        return score(res.join());
    });
}