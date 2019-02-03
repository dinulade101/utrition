const functions = require('firebase-functions');
const app = require('express')();

app.use(require('cors')({origin: true}));
const analyser = require('./analyser');

app.get('/ping', (req, response, next) => {
    analyser(['ping']).then(res => {
        response.send(res);
        return;
    }).catch(err => {
        if (err) {
            return next(err);
        }
    });
});

app.post('/analyse', (req, response) => {
    if (!req.body.ingredients) {
        throw new Error('Empty ingredients list')
    }
    analyser(req.body.ingredients).then(res => {
        console.log(res);
        response.send(res);
        return;
    }).catch(err => {
        if (err) {
            return next(err);
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
