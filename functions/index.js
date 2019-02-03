const functions = require('firebase-functions');
const app = require('express')();

app.use(require('cors')({origin: true}));
const analyser = require('./analyser');

app.get('/ping', (req, response) => {
    analyser(['ping']).then(res => {
        response.send(res);
        return;
    }).catch(err => {
        if (err) {
            console.log('ERROR', err);
            response.send(err);
        }
    })
});

// // Create and Deploy Your First Cloud Functions
// // https://firebase.google.com/docs/functions/write-firebase-functions
//
// exports.helloWorld = functions.https.onRequest((request, response) => {
//  response.send("Hello from Firebase!");
// });


exports.api = functions.https.onRequest(app);
