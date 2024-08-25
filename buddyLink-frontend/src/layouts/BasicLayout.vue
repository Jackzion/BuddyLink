<template>
  <!-- 页面标题 + 左右按钮  -->
  <van-sticky>
    <van-nav-bar
        :title="title"
        left-text="返回"
        left-arrow
        @click-left="onClickLeft"
        @click-right="onClickRight"
    >
      <template #right>
        <van-icon name="search" size="18" />
      </template>
    </van-nav-bar>
    <!--  通知栏-->
    <van-notice-bar
        left-icon="volume-o"
        text="无论我们能活多久，我们能够享受的只有无法分割的此刻，此外别无其他。"
    />
  </van-sticky>

  <!--  内容区 -- 插槽 -->
  <slot></slot>
  <!--  底部导航栏 -->
  <van-tabbar route @change="onChange">
    <van-tabbar-item to="/" icon="home-o" name="index">主页</van-tabbar-item>
    <van-tabbar-item to="/blog" icon="notes-o" name="friend">博客</van-tabbar-item>
    <van-tabbar-item to="/message" icon="comment-o" name="message">消息</van-tabbar-item>
    <van-tabbar-item to="/team" icon="flag-o" name="team">队伍</van-tabbar-item>
    <van-tabbar-item to="/user/info" icon="friends-o" name="user">用户</van-tabbar-item>
  </van-tabbar>
</template>

<script setup lang="ts">
import {useRoute, useRouter} from "vue-router";
import routes from "../config/route.ts";
import {ref} from "vue";

  const router = useRouter()
  const DEFAULT_TITLE = "Buddy Link";
  const title = ref(DEFAULT_TITLE);

  // 路由前 取 route.title 作为标题
  router.beforeEach((to, from, next) => {
    const toPath = to.path;
    // 根据 toPath 找到对应的路由信息
    const matchedRoute = routes.find((route) =>{
      return route.path === toPath;
    });
    // 根据title设置标题
    if (matchedRoute) {
      // matchedRoute 不为空 ，取 route.title 作为标题
      title.value = matchedRoute.title ?? DEFAULT_TITLE;
    }else {
      // matchedRoute 为空，取 DEFAULT_TITLE 作为标题
      title.value = DEFAULT_TITLE;
    }
    next();
  });

const onClickLeft = () => {
  router.back();
}

const onClickRight = () => {
  router.push('/search');
};

const onChange = () => {
}

</script>

<style scoped>

</style>