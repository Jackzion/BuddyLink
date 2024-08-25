<template>
  <van-divider>评论区</van-divider>
  <van-cell-group v-for="commentVO in commentVOList">
<!--    userName-->
    <van-cell center :title="commentVO.username" :label="commentVO.text" >
<!--      头像-->
      <template #icon>
        <van-image round :src="commentVO.userAvatarUrl" width="50" height="50"/>
      </template>
<!--      删除评论-->
      <template #right-icon>
        <van-icon name="delete-o" class="delete-o" v-if="commentVO.isMyComment" @click="deleteComment(commentVO.id)"/>
      </template>
    </van-cell>
  </van-cell-group>
  <div style="width: 100%; height: 60px"></div>
</template>

<script setup lang="ts">

import {CommentVOType} from "../models/commentVO";
import {showFailToast, showToast} from "vant";
import {defineEmits} from "vue";
import myAxios from "../config/myAxios.ts";

// 获取 props
interface CommentVOListProps {
  commentVOList: CommentVOType[]
}

withDefaults(defineProps<CommentVOListProps>(), {
  commentVOList : () => []
});

// 定义 emit 事件
const emit = defineEmits(['delete-comment']);

// 删除评论
const deleteComment = async (id) => {
  const res : any = await myAxios.post('/comment/delete', {
    id: id
  });
  if (res?.code === 0) {
    showToast('删除成功');
    // 通知 父组件删除成功 ，更新 commonList
    emit('delete-comment');
  } else {
    showFailToast('删除失败');
  }
};
</script>

<style scoped>
.delete-o{
  font-size: 16px;
  line-height: inherit;
}
</style>