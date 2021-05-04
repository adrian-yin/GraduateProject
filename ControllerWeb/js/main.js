/***********************地图****************************/
// 绘制地图
const map = new AMap.Map(document.getElementById('mapContainer'), {
    zoom: 18.5,
    viewMode: '2D',
    lang: 'zh_cn'
});

let marker;
function setMyLocation(longitude, latitude) {
    if (marker) {
        map.remove(marker);
    }
    marker = new AMap.Marker({
        position: new AMap.LngLat(longitude, latitude),
        title: '小车位置'
    });
    map.add(marker);
    map.setCenter([longitude, latitude]);
}

/*************************连接socket服务器*********************/
// 房间名
const roomName = "car";

const socket = io.connect('https://yinxu.monster:9123');
socket.on('connect', function() {
    socket.emit('create or join', roomName);
    socket.on('created', function() {
        console.log('成功创建房间');
    });
    socket.on('full', function() {
        console.log('加入房间失败，房间已满');
    });
    socket.on('join', function() {
        console.log('有节点加入房间');
    });
    socket.on('joined', function() {
        console.log('成功加入房间');
    });
    socket.on('log', function(args) {
        console.log('服务器日志信息' + args);
    });
    socket.on('bye', function() {
        console.log('有节点退出房间');
    });
    socket.on('message', function(args) {
        let type = args.type;
        let longitude = args.longitude;
        let latitude = args.latitude;
        console.log(type);
        switch (type) {
            case 'location':
                setMyLocation(longitude, latitude);
                break;
            default:
                break;
        }
    });

    /********************发送小车远程控制信号****************/
    // 绑定按键发送指令
    let currentCommand = '';
    function sendCommand(command) {
        if (command !== currentCommand) {
            console.log('here');
            let data = JSON.stringify({
                type: 'command',
                command: command
            });
            socket.emit('message', data);
            currentCommand = command;
        }
    }

    document.addEventListener('keydown', function(event) {
        switch (event.key) {
            case 'w': case 'W':
                sendCommand('w');
                break;
            case 'x': case 'X':
                sendCommand('x');
                break;
            case 'a': case 'A':
                sendCommand('a');
                break;
            case 'd': case 'D':
                sendCommand('d');
                break;
            default:
                break;
        }
    });
    document.addEventListener('keyup', function(event) {
        const controlKey = ['w', 'W', 'x', 'X', 'a', 'A', 'd', 'D'];
        if (controlKey.indexOf(event.key) !== -1) {
            sendCommand('s');
        }
    });
});

/******************获取远端视频*******************/
function handleError(error) {
    console.log("Error: ", error);
}

const videoContainer = document.getElementById('videoContainer');

// 将视频流添加到画面容器
function addVideoStream(elementId) {
    let streamDiv = document.createElement('div');
    streamDiv.id = elementId;
    // streamDiv.style.transform = 'rotateY(180deg)';  //视频镜像
    videoContainer.appendChild(streamDiv);
}

// 将视频流移除画面容器
function removeVideoStream(elementId) {
    let streamDiv = document.getElementById(elementId);
    videoContainer.removeChild(streamDiv);
}

// 创建通话客户端
let client = AgoraRTC.createClient({
    mode: 'rtc',
    codec: 'vp8'
});
client.init("1d4aa706feb74050a7a7eafd831bca1a", function() {
    console.log("成功初始化视频通话客户端");
}, function(error) {
    console.log("初始化视频通话客户端失败");
});

// 加入频道
client.join(
    "0061d4aa706feb74050a7a7eafd831bca1aIAABuJOCNx7K5lyWa1LIkw/lQJ/GCCYd75tu9D0UdOeV3wx+f9gAAAAAEACkGjsKJLKSYAEAAQAkspJg",
    "test",
    null,
    function(uid) {
        let localStream = AgoraRTC.createStream({
            audio: true,
            video: false
        });
        localStream.init(function() {
            client.publish(localStream, handleError);
        }, handleError);
    },
    handleError
);

// 订阅远端流
client.on('stream-added', function(event) {
    client.subscribe(event.stream, handleError);
});
// 播放远端流
client.on('stream-subscribed', function(event){
    let stream = event.stream;
    let streamId = String(stream.getId());
    addVideoStream(streamId);
    stream.play(streamId);
});

// 关闭远端流
client.on("stream-removed", function(event){
    let stream = event.stream;
    let streamId = String(stream.getId());
    stream.close();
    removeVideoStream(streamId);
});
client.on("peer-leave", function(event){
    let stream = event.stream;
    let streamId = String(stream.getId());
    stream.close();
    removeVideoStream(streamId);
});