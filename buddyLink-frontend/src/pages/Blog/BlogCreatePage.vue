<template>
  <van-field
      v-model="blog.title"
      label="标题"
      placeholder="请输入博客标题"
  />
<!--  图片上传-->
<!--  <div style="display: flex; align-items: center; justify-content: space-between">-->
<!--    <span style="margin-left: 15px">封面图片</span>-->
<!--    <van-uploader-->
<!--        v-model="coverImageList"-->
<!--        multiple-->
<!--        :max-size="500 * 1024 * 1024"-->
<!--        :max-count="1"-->
<!--        :after-read="afterRead"/>-->
<!--  </div>-->

  <van-field
      v-model="tag"
      label="标签"
      placeholder="请输入博客标签，回车添加"
      @keydown.enter="addBlogTag"
  />

  <div style="display: flex; align-items: center; justify-content: space-between; height: 40px">
    <span style="margin-left: 15px; font-size: 14px">已选标签</span>
    <div>
      <van-tag
          v-for="tag in blog.tags"
          :key="tag"
          @close="delBlogTag(tag)"
          plain
          closeable
          color="#ffe1e1"
          text-color="#ad0000"
          style="margin-left: 2px;">
        {{ tag }}
      </van-tag>
    </div>
  </div>

<!--  md 编辑器 todo : uploadImages ... https://github.com/bytedance/bytemd -->
  <Editor :value="blog.content" :mode="`split`" @change="onMdChange" :uploadImages="uploadImages" :plugins = "plugins"/>
<!--  <Editor v-model="blog.content" editorStyle="height: 320px" />-->
  <div style="margin: 0 auto; text-align: center;">
    <van-button type="success" style="width: 320px; margin-top: 10px" @click="createBlog">发布博客</van-button>
  </div>

</template>

<script setup lang="ts">
import {ref} from "vue";
import {showToast} from "vant";
import {useRouter} from "vue-router";
import myAxios from "../../config/myAxios.ts";
import {Editor} from "@bytemd/vue-next";
import gfm from "@bytemd/plugin-gfm";
import highlight from "@bytemd/plugin-highlight";

const router = useRouter();
const plugins = [gfm(), highlight()];

// Add Blog request
const blog = ref({
  title: '',
  content: '',
  coverImage: '',
  images: [],
  tags: [],
});

const tag = ref('');

const coverImageList = ref([]);

// md editor change event --- save content
const onMdChange = (v: string) => {
  console.log(v);
  blog.value.content = v;
};

// add tags
const addBlogTag = () => {
  // push data
  blog.value.tags.push(tag.value);
  // clear input
  tag.value = '';
};

// delete tags
const delBlogTag = (value: string) => {
  console.log(value);
  // filter the tags
  blog.value.tags = blog.value.tags.filter(item => {
    return item !== value;
  });
};


const afterRead = (file: File) => {
  uploadUploadCoverImage(file);
};

const uploadUploadCoverImage = async (file) => {

  showToast("图片上传成功")

  // if (file.size > 5 * 1024 * 1024) {
  //   showToast('图片大小不能超过 5M');
  //   return;
  // }
  // const formData = new FormData();
  // formData.append('file', file.file);
  // const res: any = await myAxios.post('/image/blog/coverImage/upload', formData, {
  //   headers: {
  //     'Content-Type': 'multipart/form-data',
  //   }
  // });
  // if (res?.code === 0 && res.data && res.data.length > 0) {
  //   showToast('图片上传成功');
  //   // 将图片设置到 blog 中
  //   blog.value.coverImage = res.data;
  // } else {
  //   showToast('图片上传失败' + (res.description ? `，${res.description}` : ''));
  // }
};

// add the blogs
const createBlog = async () => {
  const res : any = await myAxios.post('/blog/add', blog.value);
  if (res?.code === 0) {
    showToast('发布成功');
    router.replace({
      path: `/blog/detail/${res.data}`
    });
  } else {
    showToast('发布失败' + (res.description ? `，${res.description}` : ''));
  }
};

const uploadImages = async (files) => {
  showToast("图片上传成功");

  // todo : OS 图片上次存储
  // const results = await Promise.all(
  //     files.map(async (file) => {
  //       if (file.size > 5 * 1024 * 1024) {
  //         return {
  //           url: '图片大小不能超过 5M',
  //         };
  //       }
  //
  //       const formData = new FormData();
  //       formData.append('file', file);
  //
  //       try {
  //         const res : any = await myAxios.post('/image/blog/image/upload', formData, {
  //           headers: {
  //             'Content-Type': 'multipart/form-data',
  //           }
  //         });
  //         if (res?.code === 0) {
  //           // 将上传的图片添加至图片列表中
  //           blog.value.images.push(res.data);
  //           // 图片转链
  //           return {
  //             url: res.data,
  //           };
  //         }
  //         return {
  //           url: res.message,
  //         };
  //       } catch (error) {
  //         console.error('图片上传失败:', error);
  //         return {
  //           url: '上传失败',
  //         };
  //       }
  //     })
  // );
  //
  // return results;
};

</script>

<style scoped>

</style>