<template>
  <van-form @submit="onSubmit">
    <van-cell-group inset>
      <van-field
          v-model="addTeamData.teamName"
          name="teamName"
          label="队伍名称"
          placeholder="请输入队伍名称"
          :rules="[{ required: true, message: '请输入队伍名称' }]"
      />
      <van-field
          v-model="addTeamData.description"
          rows="4"
          autosize
          label="队伍描述"
          type="textarea"
          placeholder="请输入队伍描述"
      />

      <van-field
          v-model="addTeamData.expireTime"
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

      <van-field name="stepper" label="最大人数">
        <template #input>
          <van-stepper v-model="addTeamData.maxNum" max="10" min="3"/>
        </template>
      </van-field>

      <van-field name="radio" label="单选框">
        <template #input>
          <van-radio-group v-model="addTeamData.status" direction="horizontal">
            <van-radio name="0">公开</van-radio>
            <van-radio name="1">私有</van-radio>
            <van-radio name="2">加密</van-radio>
          </van-radio-group>
        </template>
      </van-field>
      <van-field
          v-if="Number(addTeamData.status) === 2"
          v-model="addTeamData.password"
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
</template>

<script setup>
import {useRouter} from "vue-router";
import {ref} from "vue";
import {showToast} from "vant";
import myAxios from "../../config/myAxios.ts";

const router = useRouter();

const initFormData = {
  "description": "",
  "expireTime": "",
  "maxNum": 3,
  "password": "",
  "status": 0,
  "teamName": "",
  "userId": 0,
};

// 需要用户填写的数据
const addTeamData = ref({...initFormData}); // 拓展对象运算符

const showPicker = ref(false);
const onConfirm = ({ selectedValues }) => {
  const currentTime = new Date().toLocaleTimeString();
  // join the selected values with '-' and add the current time
  addTeamData.value.expireTime = Date.parse(selectedValues.join('-') + ' ' + currentTime);
  // close pop up
  showPicker.value = false;
};

const onSubmit = async () => {
  const postData = {
    ...addTeamData.value,
    status: Number(addTeamData.value.status) // string to number
  }
  // todo 前端参数校验
  const res = await myAxios.post('/team/add', postData);
  if (res.data) {
    showToast("建队成功");
    router.push({
      path: "/team",
      replace: true
    })
  } else {
    showToast("添加失败" + (`${res.description}` ? `，${res.description}` : ''));
  }
}

</script>

<style scoped>

</style>