# ---------- 构建阶段：Node 编译 React ----------
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
# 国内网络走 npmmirror，不需要可删掉这一行
RUN npm config set registry https://registry.npmmirror.com && npm ci
COPY . .
RUN npm run build

# ---------- 运行阶段：Nginx 托管静态文件并把 /api 反代到后端 ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
