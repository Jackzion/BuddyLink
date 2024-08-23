import axios from "axios";

// todo : 可更改为上线地址
export const URL = 'http://localhost:8080/api'

const myAxios =  axios.create({
    baseURL: URL,
})

myAxios.defaults.withCredentials = true; //设置为true

// 设置拦截器，方便效验和提取 data
myAxios.interceptors.request.use(
function (config) {
    // 在发送请求之前做些什么
    console.log('我要发请求啦');
    return config;
  },
function (error) {
    // 对请求错误做些什么
    return Promise.reject(error);
  });

myAxios.interceptors.response.use(
function (response) {
    // 对响应数据做点什么
    console.log('我收到响应啦');
    console.log(response?.data.code);
    // 未登录 ， 跳转到登录页面
    if(response?.data?.code === 40100){
        // 保存当前页面地址，登录后跳转回来
        const redirectUrl = window.location.href;
        // 跳转到登录页面 ， 设置 redirectUrl 参数
        window.location.href = `/user/login?=redirectUrl=${redirectUrl}`;
    }
    return response.data;
},
function (error) {
    // 对响应错误做点什么
    return Promise.reject(error);
  });

export default myAxios;