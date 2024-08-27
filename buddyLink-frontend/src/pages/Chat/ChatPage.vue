<template>
<!--  吸顶标题栏-->
  <van-sticky>
    <van-nav-bar
        :title="title"
        left-arrow
        @click-left="onClickLeft"
    >
    </van-nav-bar>
  </van-sticky>

  <div class="chat-container">
    <!-- 显示聊天内容，使用 v-html 动态插入内容 -->
    <div class="content" ref="chatRoom" v-html="stats.content"></div>
    <!--底部 输入框 and 发送按钮-->
    <van-cell-group inset style="position: fixed;bottom: 0;width: 100%">
      <van-field
          v-model="stats.text"
          center
          clearable
          placeholder="聊点什么吧...."
          @keydown.enter="send">
        <template #button style="margin-bottom: 100px">
          <van-button size="small" type="primary" @click="send" style="margin-right: 16px">发送</van-button>
        </template>
      </van-field>
    </van-cell-group>


  </div>

</template>
<script setup>
import {nextTick, onMounted, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import {showFailToast} from "vant";

import {getCurrentUser} from "../../services/user.ts";
import myAxios, {URL} from "../../config/myAxios.ts";

const route = useRoute();
const router = useRouter();
// 定义聊天相关的状态管理
const stats = ref({
  user: {
    id: 0,
    username: "",
    avatarUrl: ''
  },
  isCollapse: false,
  users: [],
  chatUser: {
    id: 0,
    username: ''
  },
  chatEnum: {
    // 私聊
    PRIVATE_CHAT: 1,
    // 队伍聊天
    TEAM_CHAT: 2,
    // 大厅
    HALL_CHAT: 3
  },
  chatType: null,
  team: {
    teamId: 0,
    teamName: ''
  },
  text: "",          // 当前输入框中的文本
  messages: [],      // 存储所有消息
  content: ""        // 用于渲染的 HTML 内容
})

// WebSocket 相关变量
let socket = null;
const heartbeatInterval = 30 * 1000; // 心跳检测间隔时间 30 秒
let heartbeatTimer = null;

// 启动 WebSocket 的心跳检测
const startHeartbeat = () => {
  // 开启间隔任务 , 30s 发送一次 ping 报文
  heartbeatTimer = setInterval(() => {
    if (socket.readyState === WebSocket.OPEN) {
      socket.send("PING");
    }
  }, heartbeatInterval);
}

// 停止心跳检测 , 清除定时器
const stopHeartbeat = () => {
  clearInterval(heartbeatTimer);
  heartbeatTimer = null;
}

const chatRoom = ref(null)
const DEFAULT_TITLE = "聊天"
const title = ref(DEFAULT_TITLE)

onMounted(async () => {
  // 获取路由传递的参数
  let {id, username, userType, teamId, teamName, teamType} = route.query

  // 设置聊天相关的用户信息和聊天类型
  stats.value.chatUser.id = Number.parseInt(id)
  stats.value.team.teamId = Number.parseInt(teamId)
  stats.value.chatUser.username = username
  stats.value.team.teamName = teamName

  // 根据不同的聊天类型设置标题和类型
  // 私人聊天
  if (userType && Number.parseInt(userType) === stats.value.chatEnum.PRIVATE_CHAT) {
    stats.value.chatType = stats.value.chatEnum.PRIVATE_CHAT
    title.value = stats.value.chatUser.username
  }
  // 队伍聊天
  else if (teamType && Number.parseInt(teamType) === stats.value.chatEnum.TEAM_CHAT) {
    stats.value.chatType = stats.value.chatEnum.TEAM_CHAT
    title.value = stats.value.team.teamName
  }
  // 大厅聊天
  else {
    stats.value.chatType = stats.value.chatEnum.HALL_CHAT
    title.value = "公共聊天室"
  }
  // 获取当前用户信息
  stats.value.user = await getCurrentUser();


  // 获取并显示聊天记录
  if (stats.value.chatType === stats.value.chatEnum.PRIVATE_CHAT) {
    // 发送请求
    const privateMessage = await myAxios.post("/chat/privateChat",
        {
          teamId: '',
          toId: stats.value.chatUser.id,
        })
    privateMessage.data.forEach(chat => {
      if (chat.isMy === true) {
        createContent(null, chat.fromUser, chat.text)
      } else {
        createContent(chat.toUser, null, chat.text, null, chat.createTime)
      }
    })
  }

  // 大厅聊天
  if (stats.value.chatType === stats.value.chatEnum.HALL_CHAT) {
    const hallMessage = await myAxios.get("/chat/hallChat")
    hallMessage.data.forEach(chat => {
      if (chat.isMy === true) {
        createContent(null, chat.fromUser, chat.text)
      } else {
        createContent(chat.fromUser, null, chat.text, chat.isAdmin, chat.createTime)
      }
    })
  }

  // 队伍聊天
  if (stats.value.chatType === stats.value.chatEnum.TEAM_CHAT) {
    const teamMessage = await myAxios.post("/chat/teamChat",
        {
          teamId: stats.value.team.teamId
        })
    teamMessage.data.forEach(chat => {
      if (chat.isMy === true) {
        createContent(null, chat.fromUser, chat.text)
      } else {
        createContent(chat.fromUser, null, chat.text, chat.isAdmin, chat.createTime)
      }
    })
  }
  // 初始化 WebSocket
  init()
  // 滚动到最新的消息
  await nextTick()
  const lastElement = chatRoom.value.lastElementChild
  lastElement.scrollIntoView()
})

// 初始化 webSocket
const init = () => {
  let uid = stats.value.user?.id;
  if (typeof (WebSocket) == "undefined") {
    showFailToast("您的浏览器不支持WebSocket")
  } else {
    // 构建 WebSocket URL
    let socketUrl = 'ws://' + URL + '/websocket/' + uid + '/' + stats.value.team.teamId;
    // 如果已有连接，先关闭
    if (socket != null) {
      socket.close();
      socket = null;
    }
    // 开启一个websocket服务
    socket = new WebSocket(socketUrl);
    //打开事件
    socket.onopen = function () {
      startHeartbeat();
    };
    // 接收到消息时的处理
    //  浏览器端收消息，获得从服务端发送过来的文本消息
    socket.onmessage = function (msg) {
      if (msg.data === "pong") {
        return;
      }
      // 对收到的json数据进行解析，
      let data = JSON.parse(msg.data)
      if (data.error) {
        showFailToast(data.error)
        return;
      }
      // 获取在线人员信息
      if (data.users) {
        // 获取当前连接的所有用户信息，并且排除自身，自己不会出现在自己的聊天列表里
        stats.value.users = data.users.filter(user => {
          return user.id !== uid
        })
      } else {
        // 验证消息可靠性
        let flag;
        if (stats.value.chatType === data.chatType) {
          // 单聊
          flag = (uid === data.toUser?.id && stats.value.chatUser?.id === data.fromUser?.id)
        }
        if ((stats.value.chatType === data.chatType)) {
          // 大厅
          flag = (data.fromUser?.id != uid)
        }
        // 队伍
        if (stats.value.chatType === data.chatType && data.teamId && stats.value.team.teamId === data.teamId) {
          flag = (data.fromUser?.id != uid)
        }
        // 消息验证可靠后 ， 添加到消息列表
        if (flag) {
          // 添加到消息列表
          stats.value.messages.push(data)
          // 构建消息内容
          createContent(data.fromUser, null, data.text, data.isAdmin, data.createTime)
        }
        // 滚动到最新消息
        nextTick(() => {
          const lastElement = chatRoom.value.lastElementChild
          lastElement.scrollIntoView()
        })
        flag = null;
      }
    };
    //关闭事件
    socket.onclose = function () {
      stopHeartbeat();
      setTimeout(init, 5000); // 5秒后重连 , 会执行一次
    };
    //发生了错误事件
    socket.onerror = function () {
      showFailToast("发生了错误")
    }
  }
}

// 发送消息
const send = () => {
  if (stats.value.chatUser.id === 0) {
    return;
  }
  if (stats.value.chatUser.id === stats.value.user.id) {
    showFailToast("不能给自己发信息")
    return;
  }
  if (!stats.value.text.trim()) {
    showFailToast("请输入内容")
  } else {
    if (typeof (WebSocket) == "undefined") {
      showFailToast("您的浏览器不支持WebSocket")
    } else {
      // 构造消息请求
      let message = {
        fromId: stats.value.user.id,
        toId: stats.value.chatUser.id,
        text: stats.value.text,
        chatType: stats.value.chatType,
        teamId: stats.value.team.teamId,
      }
      // 发送 json message
      socket.send(JSON.stringify(message));
      // 添加到消息列表
      stats.value.messages.push({user: stats.value.user.id, text: stats.value.text})
      // 构建消息内容
      createContent(null, stats.value.user, stats.value.text)
      // 清空消息输入框
      stats.value.text = '';
      // Dom 更新后执行
      nextTick(() => {
        // 获取 chatRoom 最后一个 element
        const lastElement = chatRoom.value.lastElementChild
        // 跳转到其 view
        lastElement.scrollIntoView()
      })
    }
  }
}

// 返回
const onClickLeft = () => {
  router.back();
}

// 显示 用户信息
const showUser = (id) => {
  router.push({
    path: `/user/${id}`,
  });
}

/**
 * 这个方法是用来将 json的聊天消息数据转换成 html的。
 */
const createContent = (remoteUser, nowUser, text, isAdmin, createTime) => {
  // 当前用户消息
  let html;
  if (nowUser) {
    // nowUser 表示是否显示当前用户发送的聊天消息，绿色气泡
    html = `
    <div class="message self">
    <div class="myInfo info">
      <img :alt=${nowUser.username} class="avatar" onclick="showUser(${nowUser.id})" src=${nowUser.avatarUrl}>
    </div>
      <p class="text">${text}</p>
    </div>
`
  } else if (remoteUser) {
    // remoteUser表示远程用户聊天消息，灰色的气泡
    html = `
     <div class="message other">
      <img :alt=${remoteUser.username} class="avatar" onclick="showUser(${remoteUser.id})" src=${remoteUser.avatarUrl}>
    <div class="info">
      <span class="username">${remoteUser.username.length < 10 ? remoteUser.username : remoteUser.username}&nbsp;&nbsp;&nbsp;${createTime}</span>
      <p class="${isAdmin ? 'admin text' : 'text'}" >${text}</p>
    </div>
    </div>
`
  }
  // todo : 管理员特殊气泡
  stats.value.content += html;
}
window.showUser = showUser
</script>
<style>
.chat-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  overflow-y: auto; /* 添加垂直滚动条 */
}

.message {
  display: flex;
  align-items: center;
  margin: 10px 10px;
}

.content {
  padding-top: 22px;
  padding-bottom: 57px;
  display: flex;
  flex-direction: column
}

.self {
  align-self: flex-end;
}

.avatar {
  align-self: flex-start;
  width: 35px;
  height: 35px;
  border-radius: 50%;
  margin-right: 10px;
  margin-left: 10px;
}

.username {
  align-self: flex-start;
  text-align: center;
  max-width: 200px;
  font-size: 12px;
  color: #999;
  padding-bottom: 4px;
  white-space: nowrap;
  overflow: visible;
  background-color: #fff;
}

.info {
  display: flex;
  flex-direction: column;
  order: 2;
}

.myInfo {
  align-self: flex-start;
}

.text {
  padding: 10px;
  border-radius: 10px;
  background-color: #eee;
  word-wrap: break-word;
  word-break: break-all;
}

.other .text {
  align-self: flex-start;
}

.self .text {
  background-color: #0084ff;
  color: #fff;
}
</style>
