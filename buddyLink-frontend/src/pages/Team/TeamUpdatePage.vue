<template>
  <basic-layout>
    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
            v-model="updateTeamData.teamName"
            name="teamName"
            label="队伍名称"
            placeholder="请输入队伍名称"
            :rules="[{ required: true, message: '请输入队伍名称' }]"
        />
        <van-field
            v-model="updateTeamData.description"
            rows="4"
            autosize
            label="队伍描述"
            type="textarea"
            placeholder="请输入队伍描述"
        />

        <van-field
            v-model="updateTeamData.expireTime"
            is-link
            readonly
            name="datePicker"
            label="过期时间"
            placeholder="点击选择过期时间"
            @click="showPicker = true"
        />
        <van-popup v-model:show="showPicker" position="bottom">
          <van-date-picker @confirm="onConfirm" @cancel="showPicker = false" />
        </van-popup>

        <van-field name="radio" label="单选框">
          <template #input>
            <van-radio-group v-model="updateTeamData.status" direction="horizontal">
              <van-radio name="0">公开</van-radio>
              <van-radio name="1">私有</van-radio>
              <van-radio name="2">加密</van-radio>
            </van-radio-group>
          </template>
        </van-field>
        <van-field
            v-if="Number(updateTeamData.status) === 2"
            v-model="updateTeamData.password"
            type="password"
            name="password"
            label="队伍密码"
            placeholder="请输入队伍密码"
            :rules="[{ required: true, message: '请填写队伍密码' }]"
        />
      </van-cell-group>
      <div style="margin: 16px;">
        <van-button round block type="primary" native-type="submit">
          提交
        </van-button>
      </div>
    </van-form>
  </basic-layout>
</template>

<script setup>
import {useRoute, useRouter} from "vue-router";
import {onMounted, ref} from "vue";
import myAxios from "../../config/myAxios.ts";
import {showToast} from "vant";
import {useStore} from "vuex";
import BasicLayout from "../../layouts/BasicLayout.vue";

const router = useRouter();
const route = useRoute();
// 需要用户填写的数据
const updateTeamData = ref({});

const showPicker = ref(false);

const teamAvatar = ref();

const id = route.query.id;

const store = useStore();

// 设置过期时间
const onConfirm = ({ selectedValues }) => {
  const currentTime = new Date().toLocaleTimeString();
  updateTeamData.value.expireTime = Date.parse(selectedValues.join('-') + ' ' + currentTime);
  showPicker.value = false;
};

//获取之前队伍信息
onMounted(async () => {
  if (id <=0 ) {
    showToast("加载队伍失败");
    return ;
  }
  const res = await myAxios.get('/team/get', {
    params: {
      id,
    }
  });
  if (res?.code === 0) {
    updateTeamData.value = res.data;
  } else {
    showToast("加载队伍失败");
  }
})

// 更新队伍
const onSubmit = async () => {
  // 封装请求参数
  const postData = {
    ...updateTeamData.value,
    status: Number(updateTeamData.value.status)
  }
  // todo 前端参数校验
  // 发送请求
  const res = await myAxios.post('/team/update', postData);
  if (res.data) {
    showToast("更新成功");
    router.push({
      path: "/team",
      replace: true
    })
  } else {
    showToast("更新队伍失败" + (`${res.description}` ? `，${res.description}` : ''));
  }
}

</script>

<style scoped>

</style>