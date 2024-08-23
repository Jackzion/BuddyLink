import UserLoginPage from "../pages/User/UserLoginPage.vue";
import UserRegisterPage from "../pages/User/UserRegisterPage.vue";

const routes = [
    {path: '/user/login', title: '登录', component: UserLoginPage, meta: {layout: 'login'}},
    {path: '/user/register', title: '注册', component: UserRegisterPage, meta: {layout: 'register'}},
    {path: '/user/registerTags', title: '选择标签', component: RegisterTagsPage, meta: {layout: 'register-tags'}},
];

export default routes;