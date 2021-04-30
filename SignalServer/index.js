const fs = require('fs');
const https = require('https');
const nodeStatic = require('node-static');
const socketIO = require('socket.io');

// https证书配置
const httpsOptions = {
    key: fs.readFileSync('certs/privkey.pem'),
    cert: fs.readFileSync('certs/cert.pem')
}

// 创建https应用，监听9123端口
const app = https.createServer(httpsOptions, function(req, res) {
    new(nodeStatic.Server)().serve(req, res);
}).listen(9123);

// 创建socket服务器
const io = socketIO.listen(app);
io.sockets.on('connection', function(socket) {

    // 在此添加信令服务器逻辑

})