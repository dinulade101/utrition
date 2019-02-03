const functions = require('firebase-functions');
const app = require('express')();

app.use(require('cors')({origin: true}));
const analyser = require('./analyser');


const re_ingredient_list = /INGREDIENTS\s*:((?:[\s\w-(\/)]*,)*(?:[\s\w-(\/)]*\.)*)/i;
const re_list = /\s*(\w[\s\w-]*\w)(?:\s*\([\s\w-]*\))*[,.]/ig;

app.post('/preproc', (req, response, next) => {
    // parse the ingredient list
    //console.log(req.body)
    if (!req.body || !req.body[0]) {
        throw 'missing input list';
    }
    let raw = Object.keys(req.body).map(k => req.body[k]);
    //console.log(raw);
    //console.log(raw.join(' '));
    let str = re_ingredient_list.exec(raw.join(' '));
    //console.log(str)
    if (!str) {
        throw 'error while parsing input list';
    }
    let m = str[1];
    let list = [];
    let match;
    while (match = re_list.exec(m)) {
        if (!match[1]) {
            throw 'error while parsing input list';
        }
        list.push(match[1]);
    }

    //console.log(list);

    // analyse each ingredient
    analyser(list).then(res => {
        //console.log(res);
        response.send(res);
        return;
    }).catch(err => {
        if (err) {
            next(err);
            return;
        }
    });
});

app.post('/analyse', (req, response, next) => {
    if (!req.body.ingredients) {
        throw 'missing ingredients list'
    }
    analyser(req.body.ingredients).then(res => {
        //console.log(res);
        response.send(res);
        return;
    }).catch(err => {
        if (err) {
            next(err);
            return;
        }
    });
});

exports.api = functions.https.onRequest(app);
