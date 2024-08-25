<template>
<!--  标题栏-->
  <van-sticky>
    <van-nav-bar
        :title="`个人中心`"
        @click-right="onClickRight"
    >
      <template #right>
        <van-icon name="setting-o" size="18"/>
      </template>
    </van-nav-bar>
  </van-sticky>
<!--notice-bar-->
  <van-notice-bar
      left-icon="volume-o"
      color="#1989fa" background="#ecf9ff"
      text="左滑右滑，交友不停！这里是你的友情工厂！Link Up, Chill Out – The Ultimate Buddy Connect!"
  />
<!--  用户信息-->
  <div v-if="user" class="user-top">
    <div class="user-profile-top">
      <van-image
          round
          width="5.5rem"
          height="5.5rem"
          :src="user.avatarUrl"
          style="margin-top: 15px; margin-left: 15px; box-shadow: 0 0 15px rgba(0, 0, 0, 0.4);"
      />
      <div class="user-profile">
        <h3>{{ user.username }}</h3>
        <van-space wrap v-for="tag in JSON.parse(user.tags)">
          <van-tag color="#ffe1e1" text-color="#ad0000" style="margin-right: 4px">
            {{ tag }}
          </van-tag>
        </van-space>
      </div>
    </div>
<!--    点赞，收藏，博客，粉丝数-->
    <div class="user-profile-bottom">
      <div class="user-profile-bottom-container">
        <span class="data">{{ blogNum }}</span>
        <span class="data-text">博客</span>
      </div>
      <div class="user-profile-bottom-container">
        <span class="data">{{ likeBlogNum }}</span>
        <span class="data-text">点赞</span>
      </div>
      <div class="user-profile-bottom-container">
        <span class="data">{{ starBlogNum }}</span>
        <span class="data-text">收藏</span>
      </div>
      <div class="user-profile-bottom-container">
        <span class="data">{{ followNum }}</span>
        <span class="data-text">粉丝</span>
      </div>
    </div>
  </div>
<!--  查看点赞，收藏，关注，粉丝列-->
  <div v-if="user" class="user-middle">
    <div class="image-container">
      <img class="icon" src="/src/assets/like.png" @click="toMyLikeBlogPage"/>
      <span class="data-text2">点赞</span>
    </div>
    <div class="image-container">
      <img class="icon" src="/src/assets/star.png" @click="toMyStarBlogPage"/>
      <span class="data-text2">收藏</span>
    </div>
    <div class="image-container">
      <img class="icon" src="/src/assets/follow.png" @click="toFollow(CardListType.FOLLOW)"/>
      <span class="data-text2">关注</span>
    </div>
    <div class="image-container">
      <img class="icon" src="/src/assets/fan.png" @click="toFollow(CardListType.FAN)"/>
      <span class="data-text2">粉丝</span>
    </div>
  </div>

  <div v-if="user" class="user-bottom">
    <div style="display: flex; justify-content: space-around; padding-top: 30px">
      <div class="image-container">
        <img class="icon" src="/src/assets/blog.png" @click="toBlogPage"/>
        <span class="data-text2">博客</span>
      </div>
      <div class="image-container">
        <img class="icon" src="/src/assets/record.png" @click="toViewedBlogPage"/>
        <span class="data-text2">阅读记录</span>
      </div>
      <div class="image-container">
        <img class="icon" src="/src/assets/teamjoined.png" @click="toMyTeamJoinPage"/>
        <span class="data-text2">已加队伍</span>
      </div>
      <div class="image-container">
        <img class="icon" src="/src/assets/teamcreated.png" @click="toMyTeamCreatePage"/>
        <span class="data-text2">已建队伍</span>
      </div>
    </div>

    <div style="display: flex; justify-content: space-around; padding-top: 30px">
      <div class="image-container">
        <img class="icon" src="/src/assets/signin.png" @click="toSignInPage"/>
        <span class="data-text2">签到</span>
      </div>
      <div class="image-container">
        <img class="icon" src="/src/assets/rank.png" @click="toRankPage"/>
        <span class="data-text2">排名</span>
      </div>
      <div class="image-container">
        <img class="icon" src="/src/assets/activity.png" @click="toActivityPage"/>
        <span class="data-text2">创作活动</span>
      </div>
      <div class="image-container">
        <img class="icon" src="/src/assets/feedback.png" @click="toFeedbackPage"/>
        <span class="data-text2">意见反馈</span>
      </div>
    </div>
  </div>

<!--  todo: meta 控制 basic layout 部分组件显示 ，以此复用-->
  <div style="margin-bottom: 54px; text-align: center;">
    <van-button type="primary" style="width: 320px; margin-top: 10px" @click="userLogout">退出登录</van-button>
  </div>
  <van-sticky>
    <van-tabbar route @change="onChange">
      <van-tabbar-item to="/" icon="home-o" name="index">主页</van-tabbar-item>
      <van-tabbar-item to="/blog" icon="notes-o" name="friend">博客</van-tabbar-item>
      <van-tabbar-item to="/message" icon="comment-o" name="message">消息</van-tabbar-item>
      <van-tabbar-item to="/team" icon="flag-o" name="team">队伍</van-tabbar-item>
      <van-tabbar-item to="/user" icon="friends-o" name="user">用户</van-tabbar-item>
    </van-tabbar>
  </van-sticky>
</template>

<script setup lang="ts">
import {onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import {showToast} from "vant";
import {getCurrentUser} from "../../services/user.ts";
import myAxios from "../../config/myAxios.ts";
import {CardListType} from "../../enums/CardListType.ts";

const router = useRouter();
const user = ref();

const followNum = ref(0);
const blogNum = ref(0);
const starBlogNum = ref(0);
const likeBlogNum = ref(0);

onMounted(async () => {
  // 获取 userVO
  user.value = await getCurrentUser();
  // 获取 userBlogVO
  const res: any = await myAxios.get('/user/info/get');
  if (res?.code === 0) {
    blogNum.value = res.data.blogNum;
    followNum.value = res.data.followNum;
    starBlogNum.value = res.data.starBlogNum;
    likeBlogNum.value = res.data.likeBlogNum;
  }
});

// 跳转到关注页面
const toFollow = (type: number) => {
  router.push({
    path: '/user/follow',
    query: {
      type: type,
    }
  })
};

// 跳转到我的博客页面
const toBlogPage = () => {
  router.push({
    path: `/user/${user.value.id}/blog`
  });
};

// 跳转到我加入的队伍页面
const toMyTeamJoinPage = () => {
  router.push({
    path: '/user/team/join'
  });
};

// 跳转到我创建的队伍页面
const toMyTeamCreatePage = () => {
  router.push({
    path: '/user/team/create'
  });
};

// 跳转到我的喜欢博客页面
const toMyLikeBlogPage = () => {
  router.push({
    path: '/user/like/blog'
  });
};

// 跳转到我的收藏博客页面
const toMyStarBlogPage = () => {
  router.push({
    path: '/user/star/blog'
  });
};

// 跳转到浏览过的博客页面
const toViewedBlogPage = () => {
  router.push({
    path: '/user/viewed/blog'
  })
}

// to score Rank Page
const toRankPage = () => {
  router.push({
    path: '/rank'
  });
};

// to singIn Page
const toSignInPage = () => {
  router.push({
    path: '/user/sign/in'
  })
};

// to FeedBack Page
const toFeedbackPage = () => {
  router.push({
    path: '/buddy/feedback'
  })
};

// todo : none
const toActivityPage = () => {
  router.push('/buddy/activity');
};

// 退出登录
const userLogout = async () => {
  // 请求
  const res: any = await myAxios.post('/user/logout');
  // 成功后返回 login page
  if (res?.code === 0) {
    showToast("退出成功");
    router.push({
      path: '/user/login'
    })
  } else {
    showToast("退出失败" + (`${res.description}` ? `，${res.description}` : ''));
  }
};

// 前往 user update page
const onClickRight = () => {
  router.push('/user/update');
};

</script>

<style scoped>
.user-top {
  width: 90%;
  height: 180px;
  margin-left: auto;
  margin-right: auto;
  margin-top: 12px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  border-radius: 5px;
}

.user-profile-top {
  display: flex;
}

.user-profile {
  margin-top: 10px;
  margin-left: 10px;
  background: white;
  width: 216px;
  height: 100px;
}

.user-profile-bottom {
  width: 100%;
  height: 70px;
  display: flex;
  justify-content: space-around;
}

.data {
  font-size: 20px;
}

.data-text {
  font-size: 12px;
  color: darkgrey;
}

.user-middle {
  width: 90%;
  height: 75px;
  margin-left: auto;
  margin-right: auto;
  margin-top: 15px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-around;
}

.icon {
  width: 40px;
  height: 40px;
}

.data-text2 {
  color: #4183F1;
}

.image-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.user-bottom {
  width: 90%;
  height: 225px;
  margin-left: auto;
  margin-right: auto;
  margin-top: 15px;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
}

.user-profile-bottom-container {
  width: 50px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

</style>