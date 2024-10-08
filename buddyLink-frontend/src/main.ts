import { createApp } from 'vue'
import App from './App.vue'
// 1. 引入你需要的组件
import {
    Button, Icon, NavBar, Tabbar, TabbarItem, Tag, Divider, TreeSelect,
    Row, Col, Cell, CellGroup, Form, Field, Stepper, Search, PickerGroup, Dialog, Sticky
} from 'vant';
import { DatePicker } from 'vant';
import * as VueRouter from 'vue-router';
import routes from "./config/route.ts";
import BasicLayout from "./layouts/BasicLayout.vue";
import store from "./config/store.ts";
import UserLoginPage from "./pages/User/UserLoginPage.vue";
import UserRegisterPage from "./pages/User/UserRegisterPage.vue";
import RegisterTagsPage from "./pages/User/RegisterTagsPage.vue";
import RegisterImagePage from "./pages/User/RegisterImagePage.vue";
import 'bytemd/dist/index.css'


const router = VueRouter.createRouter({
    // 4. 内部提供了 history 模式的实现。为了简单起见，我们在这里使用 hash 模式。
    history: VueRouter.createWebHistory(),
    routes: routes, // `routes: routes` 的缩写
})

const app= createApp(App);
app.use(Button);
app.use(NavBar);
app.use(Icon);
app.use(Tabbar);
app.use(TabbarItem);
app.use(Tag);
app.use(Divider);
app.use(TreeSelect);
app.use(Row);
app.use(Col);
app.use(Cell);
app.use(CellGroup);
app.use(Form);
app.use(Field);
app.use(Button);
app.use(Stepper);
app.use(DatePicker);
app.use(Search);
app.use(PickerGroup);
app.use(store);
app.use(Dialog);
app.use(Sticky);
// 设置全局组件...
app.component("default-layout", BasicLayout);
app.component("login-layout", UserLoginPage);
app.component("register-layout", UserRegisterPage);
app.component("register-tags-layout", RegisterTagsPage);
app.component("register-image-layout", RegisterImagePage);

app.use(router);
app.mount('#app')