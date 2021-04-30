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

    let that = this;

    //记录加入房间号
    let roomName;

    // 向客户端发送服务器日志信息
    function log() {
        const array = ['来自服务器的信息：'];
        array.push.apply(array, arguments);
        socket.emit('log', array);
        console.log('向客户端发送日志信息：', array)
    }

    // 收到消息转发
    socket.on('message', function(message) {
        log('收到客户端信息：', message);
        // 房间内转发消息
        if (!roomName) {
            console.log('消息转发失败，连接未加入房间');
        } else {
            console.log('在房间' + roomName + '内转发消息：' + message);
            socket.to(roomName).emit('message', message);
        }
    });

    socket.on('create or join', function(roomName) {
        log('收到创建或加入房间的请求，房间名：' + roomName);

        let clientsInRoom = io.sockets.adapter.rooms[roomName];
        let numClients = clientsInRoom ? Object.keys(clientsInRoom.socket).length : 0;
        log(roomName + '房间目前有' + numClients + '个客户端');

        if (numClients === 0) {
            // 房间不存在，创建房间
            socket.join(roomName);
            that.roomName = roomName;
            log('用户' + socket.id + '创建了房间' + roomName);
            socket.emit('created', roomName, socket.id);
        } else if (numClients === 1) {
            // 房间已有1个客户端，加入房间
            log('用户' + socket.id + '加入了房间' + roomName);
            io.sockets.in(roomName).emit('join', roomName);
            socket.join(roomName);
            that.roomName = roomName;
            socket.emit('joined', roomName, socket.id);
            io.sockets.in(roomName).emit('ready');
        } else {
            // 房间已满
            socket.emit('full', roomName);
        }
    });

    socket.on('ipaddr', function() {
        const ifaces = os.networkInterfaces();
        for (let dev in ifaces) {
            if (!ifaces.hasOwnProperty(dev)) {
                break;
            }
            ifaces[dev].forEach(function(details) {
                if (details.family === 'IPv4' && details.address !== '127.0.0.1') {
                    socket.emit('ipaddr', details.address);
                }
            });
        }
    });

    socket.on('bye', function () {
        console.log('收到关闭连接信息');
    });
})