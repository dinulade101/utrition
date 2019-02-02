const functions = require('firebase-functions');
const app = require('express')();

app.use(require('cors')({origin: true}));

// google cloud client libraries
const language = require('@google-cloud/language');
const client = new language.LanguageServiceClient();

app.get('/ping', (req, res) => {
    client.analyzeSentiment({
        document: {
            content: 'hello world!',
            type: 'PLAIN_TEXT'
        }
    }).then(results => {
        res.send({
            pong: results
        });
        return;
    }).catch(err => {
        if (err) {
            res.send({
                pong: err
            });
        }
    });
});

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
// });


exports.api = functions.https.onRequest(app);
