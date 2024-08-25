export enum BlogType {
    CreatedBySelf = -1, // 查自己创建的
    FavoritedBySelf = 0, // 查自己收藏的
    LikedBySelf = 1, // 查自己点赞的
    ReadBySelf = 2, // 查自己的阅读过的文章
    FavoritedByOthers = 3, // 查别人收藏的
    LikedByOthers = 4, // 查别人点赞的
    ReadByOthers = 5 // 查别人的阅读过的文章
}