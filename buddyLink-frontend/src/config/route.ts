import UserLoginPage from "../pages/User/UserLoginPage.vue";
import UserRegisterPage from "../pages/User/UserRegisterPage.vue";
import RegisterTagsPage from "../pages/User/RegisterTagsPage.vue";
import RegisterImagePage from "../pages/User/RegisterImagePage.vue";
import Index from "../pages/Index.vue";
import UserIntroPage from "../pages/User/UserIntroPage.vue";
import UserPage from "../pages/User/UserPage.vue";
import BlogPage from "../pages/Blog/BlogPage.vue";
import UserTeamJoinPage from "../pages/Team/UserTeamJoinPage.vue";
import TeamUpdatePage from "../pages/Team/TeamUpdatePage.vue";
import ChatPage from "../pages/Chat/ChatPage.vue";
import UserTeamCreatePage from "../pages/Team/UserTeamCreatePage.vue";
import BlogDetailPage from "../pages/Blog/BlogDetailPage.vue";
import UserBlogPage from "../pages/Blog/UserBlogPage.vue";
import SearchResultPage from "../pages/User/SearchResultPage.vue";
import SearchPage from "../pages/User/SearchPage.vue";
import UserStarBlogPage from "../pages/User/UserStarBlogPage.vue";
import UserLikeBlogPage from "../pages/User/UserLikeBlogPage.vue";
import UserViewedBlogPage from "../pages/User/UserViewedBlogPage.vue";
import RankPage from "../pages/Other/RankPage.vue";
import SignInPage from "../pages/Other/SignInPage.vue";
import FeedbackPage from "../pages/Other/FeedbackPage.vue";
import ActivityPage from "../pages/Other/ActivityPage.vue";



const routes = [
    {path: '/user/login', title: '登录', component: UserLoginPage},
    {path: '/user/register', title: '注册', component: UserRegisterPage},
    {path: '/user/registerTags', title: '选择标签', component: RegisterTagsPage},
    {path: '/user/registerImage', title: '选择头像', component: RegisterImagePage},
    {path: '/', component: Index},
    {path: '/user/intro', title: '用户详情', component: UserIntroPage},
    {path: '/user/info', title: '个人中心', component: UserPage},
    {path: '/user/:id/blog', title: 'Ta 的博客', component: BlogPage},
    {path: '/user/team/join', title: '我加入的队伍', component: UserTeamJoinPage},
    {path: '/team/update', title: '队伍信息修改', component: TeamUpdatePage},
    {path: '/chat', title: '聊天界面', component: ChatPage},
    {path: '/user/team/create', title: '我创建的队伍', component: UserTeamCreatePage},
    {path: '/blog/detail/:id', title: '博客详情', component: BlogDetailPage},
    {path: '/user/:id', title: 'Ta 的个人主页', component: UserBlogPage},
    {path: '/search', title: '搜索', component: SearchPage},
    {path: '/user/listByTag', title: '搜索结果', component: SearchResultPage},
    {path: '/user/star/blog', title: '我收藏的博客', component: UserStarBlogPage},
    {path: '/user/like/blog', title: '我点赞的博客', component: UserLikeBlogPage},
    {path: '/user/viewed/blog', title: '浏览过的的博客', component: UserViewedBlogPage},
    {path: '/rank', title: '排行榜', component: RankPage},
    {path: '/user/sign/in', title: '签到', component: SignInPage},
    {path: '/buddy/feedback', title: '反馈', component: FeedbackPage},
    {path: '/buddy/activity', title: '创作活动', component: ActivityPage},

];

export default routes;