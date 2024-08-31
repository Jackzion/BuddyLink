<template>
  <div id="rank-echart" v-show="dataList.length > 0"/>
  <van-empty v-show="!dataList || dataList.length < 1" description="还没有用户上榜哦" />
</template>

<script setup lang="ts">
import * as echarts from 'echarts';
import {onMounted, ref} from "vue";
import {showToast} from "vant";
import myAxios from "../../config/myAxios.ts";
const dataList = ref([]);

// 动态折线图 参考 https://echarts.apache.org/examples/zh/editor.html?c=bar-race&lang=ts
onMounted(async () => {
  // 获取 HTML 元素中 id 为 'rank-echart' 的元素，这个元素将用作 ECharts 图表的容器
  const chartDom = document.getElementById('rank-echart');

  // 初始化 ECharts 实例，将图表绑定到上面获取的 DOM 元素中
  const myChart = echarts.init(chartDom);

  // 定义一个变量来存储图表的配置选项
  let option;

  // 使用 `myAxios` 发送 GET 请求，获取用户的积分排名数据
  const res: any = await myAxios.get('/user/score/rank');

  // 检查响应结果，如果响应码为 0，表示请求成功
  if (res?.code === 0) {
    // 获取用户列表数据
    const userList = res.data;

    // 将获取到的数据存储到 dataList 中
    dataList.value = res.data;

    // 定义两个数组分别存储用户名和积分
    const usernameList: string[] = [];
    const scoreList: number[] = [];

    // 遍历用户列表，将每个用户的用户名和积分分别存入相应的数组中
    userList.forEach(user => {
      usernameList.push(user.username);
      scoreList.push(user.score);
    });

    // 配置图表的选项
    option = {
      title: {
        text: '用户排名' // 图表的标题
      },
      tooltip: {
        trigger: 'axis', // 鼠标移动到图表时显示提示信息
        axisPointer: {
          type: 'shadow' // 使用阴影指示器
        }
      },
      legend: {}, // 图表的图例
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true // 调整网格以包含所有标签
      },
      xAxis: {
        type: 'value', // X 轴为数值类型
        boundaryGap: [0, 0.01] // X 轴的边界间隙
      },
      yAxis: {
        type: 'category', // Y 轴为类别类型
        data: usernameList, // 使用用户名列表作为 Y 轴的数据
      },
      series: [
        {
          name: '用户积分榜', // 数据系列的名称
          type: 'bar', // 数据系列的类型为柱状图
          data: scoreList, // 使用积分列表作为数据系列的数据
        }
      ]
    };

    // 如果 `option` 存在，则将其应用到 ECharts 图表中
    option && myChart.setOption(option);

  } else {
    // 如果请求失败，显示一个提示信息
    showToast('加载失败');
  }

});


</script>

<style scoped>
#rank-echart{
  width: 380px;
  height: 500px;
}
</style>