<template>
<!--  滑动单元格-->
  <van-swipe-cell v-for="blog in blogList">
<!--    blog info-->
    <van-card
        :desc="blog.content"
        :title="blog.title"
        class="blog-card"
        :thumb="blog.coverImage"
        @click="toBlog(blog.id)">
      <template #tags>
        <van-tag
            plain
            color="#ffe1e1"
            text-color="#ad0000"
            style="margin-right: 8px;"
            v-for="tag in JSON.parse(blog.tags)">
          {{ tag }}
        </van-tag>
      </template>
    </van-card>
<!--    control button-->
    <template #right v-if="blog.blogUserVO.id === currentUser.id">
      <van-button round text="删除" type="danger" class="button" @click="deleteBlog(blog.id)"
                  v-if="blog.blogUserVO.id === currentUser.id"/>
      <van-button round text="修改" type="success" class="button" @click="toEditBlog(blog.id)"
                  v-if="blog.blogUserVO.id === currentUser.id"/>
      <van-button round text="点赞" type="success" class="button" @click="likeBlog(blog.id, blog.isLiked)"
                  v-if="blog.blogUserVO.id !== currentUser.id"/>
      <van-button round text="收藏" type="success" class="button" @click="starBlog(blog.id, blog.isStarred)"
                  v-if="blog.blogUserVO.id !== currentUser.id"/>
    </template>

  </van-swipe-cell>

</template>

<script setup lang="ts">
import {useRouter} from "vue-router";
import {getCurrentUser} from "../services/user";
import {showToast} from "vant";
import {onMounted, ref} from "vue";
import myAxios from "../config/myAxios.ts";
import {BlogType} from "../models/blog";

const router = useRouter();

const currentUser = ref();

const emit = defineEmits(['delete-blog']);

// 渲染时先获取 CurrentUser
onMounted(async () => {
  currentUser.value = currentUser.value = await getCurrentUser();
  console.log(currentUser.value?.id);
});

// 删除博客
const deleteBlog = async (id: number) => {
  const res: any = await myAxios.post('/blog/delete', {
    id: id,
  });
  if (res?.code === 0) {
    showToast('删除成功');
    // 发送 delete-blog 事件 , 通知 父组件刷新
    emit('delete-blog');
  } else {
    showToast('删除失败');
  }
};

// 编辑博客
const toEditBlog = (id: number) => {
  router.push(`/blog/edit/${id}`);
};

// 收藏blog
const starBlog = async (id: number, isStarred: boolean) => {
  const res: any = await myAxios.post('/blog/star', {
    blogId: id,
    isStarred: isStarred
  });
  if (res?.code === 0) {
    showToast('收藏成功');
  } else {
    showToast('收藏失败');
  }
};

// 点赞 blog
const likeBlog = async (id: number, isLiked: boolean) => {
  const res: any = await myAxios.post('/blog/like', {
    blogId: id,
    isLiked: isLiked
  });
  if (res?.code === 0) {
    showToast('点赞成功');
  } else {
    showToast('点赞失败');
  }
};

// 跳转到博客详情
const toBlog = (id) => {
  console.log(id);
  router.push({
    path: `/blog/detail/${id}`
  });
};

interface BlogListType {
  blogList: BlogType[];
}

withDefaults(defineProps<BlogListType>(), {
});

</script>

<style scoped>
.button {
  height: 100%;
}

</style>