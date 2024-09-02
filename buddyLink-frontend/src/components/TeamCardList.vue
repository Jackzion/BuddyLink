<template>
  <div class="team-card-list">
    <van-card
        v-for ="team in teamList"
        :tag="teamStatusEnum[team.status]"
        price="2.00"
        :desc="team.description"
        :title="team.teamName"
        thumb="https://fastly.jsdelivr.net/npm/@vant/assets/ipad.jpeg"
        @click = "doTeamIntro(team)"
    >
    <template #footer>
      <van-button v-if="team.userId !== currentUser?.id && !team.hasJoin" size="normal" type="primary"  @click.stop="preDoJoinTeam(team)">加入</van-button>
      <van-button  v-if="team.hasJoin && team.userId === currentUser?.id" size="normal" type="success" @click.stop = "doUpdateTeam(team.id)">更新队伍</van-button>
      <van-button v-if="team.hasJoin && team.userId !== currentUser?.id" size="normal" type="warning" @click.stop="doQuitTeam(team.id)">退出</van-button>
      <van-button v-if="team.hasJoin && team.userId === currentUser?.id" size="normal" type="warning" @click.stop="doDeleteTeam(team.id)">解散</van-button>
    </template>
<!--      队伍头像列表-->
    <template #price>
      <van-image v-for = "user in maxFourUsers(team)" style="margin: 5px"
          round
          width="2rem"
          height="2rem"
          :src="user.avatarUrl"
      />
      <div v-if="team.hasJoinNum > 4">
        。。。。
      </div>
    </template>
    <template #bottom>
      <div>
        {{ `队伍人数: ${team.hasJoinNum}/${team.maxNum}`}}
      </div>
      <div v-if="team.expireTime">
        {{ '过期时间' + team.expireTime}}
      </div>
      <div v-if="team.createTime">
        {{ '创建时间' + team.createTime}}
      </div>
    </template>
    </van-card>
    <!--输入密码框-->
    <van-dialog v-model:show="showPasswordDialog" title="请输入队伍密码" show-cancel-button @confirm="doJoinTeam" @cancel="cancelJoin">
      <van-field v-model="password" placeholder="请输入队伍密码" />
    </van-dialog>
  </div>
</template>

<script setup lang="ts">

  import {TeamType} from "../models/team";
  import {teamStatusEnum} from "../enums/TeamType.ts";
  import {computed, onMounted, ref} from "vue";
  import {getCurrentUser} from "../services/user.ts";
  import myAxios from "../config/myAxios.ts";
  import {showFailToast, showSuccessToast} from "vant";
  import {useRoute, useRouter} from "vue-router";

  const router = useRouter();

  // 获取props
  interface props{
    teamList: TeamType[]
  }
  defineProps<props>();

  const currentUser = ref();

  onMounted( async () => {
    // 获取 currentUser
    currentUser.value = await getCurrentUser();
  });

  // 最多显示四个头像
  const maxFourUsers = (team: TeamType) => {
    return team.userList.slice(0,4);
  }

  const showPasswordDialog = ref(false);
  const password = ref();
  const joinTeamId = ref();
  const preDoJoinTeam = (team: TeamType) => {
    joinTeamId.value = team.id;
    // 判断是否需要弹出密码 dialog
    if(team.status == 0){
      doJoinTeam();
    }else{
      showPasswordDialog.value = true;
    }
  }

  // 加入队伍
  const doJoinTeam = async () => {
    // 调用接口
    const res = await myAxios.post("/team/join" , {
      teamId: joinTeamId.value,
      password: password.value
    });
    if(res?.code == 200){
      // 更新 teamList
      showSuccessToast("加入成功");
      //  跟新 teamList
      emit('update-teams');
      cancelJoin();
    }else {
      showFailToast("加入失败" + (res.description ? `， ${res.description} `:''));
    }
  }

  const cancelJoin = () => {
    // 关闭 dialog
    showPasswordDialog.value = false;
  }

  /**
   * 跳到更新队伍页
   */
  const doUpdateTeam = (id: number) => {
    router.push({
      path: '/team/update',
      query: {
        id: id,
      }
    })
  }

  // 退出队伍
  const doQuitTeam = async(id: number) => {
    const res = await myAxios.post('/team/quit' , {
      teamId: id
    });
    if (res?.code === 0) {
      showSuccessToast("退出成功");
      //  跟新 teamList
      emit('update-teams');
    } else {
      showFailToast("退出失败");
    }
  }

  // 解散队伍
  const doDeleteTeam = async(id: number) => {
    const res = await myAxios.post('/team/delete' , {
      id
    });
    if (res?.code === 0) {
      showSuccessToast("解散成功");
      //  跟新 teamList
      emit('update-teams');
    } else {
      showFailToast("解散失败");
    }
  }

  /**
   * 点击跳转队伍详情页
   * @param val
   */
  const doTeamIntro = (team) => {
    router.push({
      path: '/team/introduce',
      query: {
        teamName: team.teamName,
        description: team.description,
        expireTime: team.expireTime,
        maxNum: team.maxNum,
        status: team.status,
        createTime: team.createTime,
        createUserName: team.createUser?.username,
        hasJoinNum: team.hasJoinNum,
      }
    })
  }

  // 父组件更新事件
  const emit = defineEmits(['update-teams']);

</script>

<style scoped>

</style>