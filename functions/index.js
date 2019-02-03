const functions = require('firebase-functions');
const app = require('express')();

app.use(require('cors')({origin: true}));
const analyser = require('./analyser');

app.post('*', (req, response) => {
    if (!req.body.ingredients) {
        throw new Error('Empty ingredients list')
    }
    analyser(req.body.ingredients).then(res => {
        console.log(res);
        response.send(res);
        return;
    }).catch(err => {
        if (err) {
            next(err);
            return;
        }
    });
});

exports.analyse = functions.https.onRequest(app);
