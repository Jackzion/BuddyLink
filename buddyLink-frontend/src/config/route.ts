import UserLoginPage from "../pages/User/UserLoginPage.vue";
import UserRegisterPage from "../pages/User/UserRegisterPage.vue";
import RegisterTagsPage from "../pages/User/RegisterTagsPage.vue";
import RegisterImagePage from "../pages/User/RegisterImagePage.vue";
import Index from "../pages/Index.vue";

const routes = [
    {path: '/user/login', title: '登录', component: UserLoginPage, meta: {layout: 'login'}},
    {path: '/user/register', title: '注册', component: UserRegisterPage, meta: {layout: 'register'}},
    {path: '/user/registerTags', title: '选择标签', component: RegisterTagsPage, meta: {layout: 'register-tags'}},
    {path: '/user/registerImage', title: '选择头像', component: RegisterImagePage, meta: {layout: 'register-image'}},
    {path: '/', component: Index},
];

export default routes;