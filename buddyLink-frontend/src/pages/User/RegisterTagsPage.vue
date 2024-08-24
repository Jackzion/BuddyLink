<template>
  <van-sticky>
<!--    标题-->
    <van-nav-bar
        :title="`至少选一个标签`"
        left-arrow
        @click-left="onClickLeft"
    >
      <template #right>
        <van-icon name="search" size="18"/>
      </template>
    </van-nav-bar>
<!--    搜索栏-->
    <form action="/">
      <van-search
          v-model="searchText"
          show-action
          placeholder="请输入搜索关键词"
          @search="onSearch"
          @cancel="onCancel"
      />
    </form>
<!--    已选标签 -->
    <van-divider content-position="left">已选标签</van-divider>
    <div v-if="activeIds.length === 0">请选择您的标签</div>
    <van-row :gutter="[10, 8]" style="padding: 0 16px;">
      <van-col v-for="tag in activeIds" >
        <van-tag closeable size="medium" type="primary" style=" margin: 3px" @close="doClose(tag)">
          {{tag}}
        </van-tag>
      </van-col>
    </van-row>
    <!--    选择标签 -->
    <van-divider content-position="left">选择标签</van-divider>
    <van-tree-select
        v-model:active-id="activeIds"
        v-model:main-active-index="activeIndex"
        :items="tagList"
    />
    <div style="display: flex; align-items: center">
      <van-button block type="primary" style="margin: 12px" @click="doSearchResult" v-if="!isShow">注册</van-button>
      <van-button block type="primary" style="margin: 12px" @click="editUserTags" v-if="isShow">修改</van-button>
    </div>
  </van-sticky>
</template>


<script setup lang="ts">

import {onMounted, ref} from "vue";
import myAxios from "../../config/myAxios.ts";
import {showSuccessToast, showToast} from "vant";
import {useRoute, useRouter} from "vue-router";
import {TagsType} from "../../enums/TagsType.ts";

const router = useRouter();
const route = useRoute();

const { type } = route.query;
const { userId } = route.query;
const  isShow  = ref(false);
const searchText = ref('');

onMounted(() => {
  if(parseInt(type) === TagsType.EDIT){
    // 修改
    isShow.value = true;
  }else{
    // 注册
    isShow.value = false;
  }
})
let originTagList = [
  {
    text: '专业',
    children: [
      { text: 'Java', id: 'java' },
      { text: 'C++', id: 'c++' },
      { text: 'Go', id: 'go' },
      { text: '嵌入式', id: '嵌入式' },
      { text: 'Python', id: 'python' },
    ],
  },
  {
    text: '年级',
    children: [
      { text: '大一', id: '大一' },
      { text: '大二', id: '大二' },
      { text: '大三', id: '大三' },
      { text: '大四', id: '大四' },
      { text: '研一', id: '研一' },
      { text: '研二', id: '研二' },
      { text: '研三', id: '研三' },
    ],
  },
  {
    text: '性别',
    children: [
      { text: '男', id: '男' },
      { text: '女', id: '女' },
    ],
  },
  {
    text: '状态',
    children: [
      { text: '单身', id: '单身' },
      { text: '有对象', id: '有对象' },
      { text: '已婚', id: '已婚' },
      { text: 'emo', id: 'emo' },
      { text: '内卷', id: '内卷' },
    ],
  },
];
let tagList = ref(originTagList);

// 父标签有所保留，遍历每个父标签 ， 遍历其子标签数组进行过滤，返回新的tagList
const onSearch = () =>{
  // 遍历父标签
  tagList.value = originTagList.map(parentTag =>{
    if(Array.isArray(parentTag.children)&& parentTag.children.length > 0){
      // 对 子标签进行过滤
      const tempChildren = parentTag.children.filter(childTag => childTag.text.includes(searchText.value));
      const tempParentTag = {...parentTag, children: tempChildren};
      return tempParentTag;
    }
    return parentTag;
  })
}
// 已选中标签
const activeIds = ref([]); // 子标签数组
const activeIndex = ref(0); // 父标签

// 删除选中标签
const doClose = (tag) =>{
  activeIds.value = activeIds.value.filter(item => {
    return item !== tag;
  })
}

// 取消搜索
const onCancel = () => {
  searchText.value = '';
  tagList.value = originTagList;
}

// 获得表单
const { registerUser } = route.query;

// 注册
const doSearchResult = () =>{

  if(activeIds.value.length >0){
    // add new data -- tagNameList
    const registerUserParam = JSON.parse(registerUser);
    const newRegisterUserParam = {
      ...registerUserParam,
      tagNameList: activeIds.value
    };
    // 路由到 Avatar select page
    router.push({
      path: '/user/registerImage',
      query: {
        registerUser: JSON.stringify(newRegisterUserParam),
      }
    });
  }else{
    showToast("请至少选择一个标签");
  }
}

// 修改用户标签
const editUserTags = async () =>{
  if(activeIds.value.length > 0){
    // 发送请求
    const res = await myAxios.post('/user/update', {
      tags: activeIds.value,
      id : parseInt(userId)
    });
    if(res?.code === 0){
      showSuccessToast("修改成功");
      showToast('修改成功');
      router.back(); // 返回上一页面
    }else{
      showToast('修改失败' + (`${res.description}` ? `，${res.description}` : ''));
    }
  }
  else{
    showToast("请至少选择一个标签");
  }
}

// 返回上个页面
const onClickLeft = () => {
  router.back();
}

</script>

<style lang="scss" scoped>

</style>