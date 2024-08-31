import {BlogUserType} from "./blogUser";
import {CommentVOType} from "./commentVO";
// team 详情
export type TeamType = {
    id: number;
    teamName: string;
    description: string;
    expireTime?: Date;
    maxNum: number;
    password?: string;
    userId: number;
    hasJoin: boolean;
    // todo定义为枚举类型
    status: number;
    createTime: Date;
    updateTime: Date;
    createUser?: UserType;
    hasJoinNum?: number;
}