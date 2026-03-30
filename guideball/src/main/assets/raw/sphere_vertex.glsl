// 输入
// mvp矩阵
uniform mat4 u_mvpMatrix;
// 模型矩阵
uniform mat4 u_modelMatrix;
// 顶点
attribute vec4 a_position;
// 法向量
attribute vec3 a_normal;
// 输出
// 世界空间的法向量
varying vec3 v_nWorld;
// 世界坐标
varying vec3 v_worldPos;

void main() {
    // 世界坐标
    vec4 worldPos = u_modelMatrix * a_position;
    v_worldPos = worldPos.xyz;
    // 世界法向量
    vec3 nw = normalize(mat3(u_modelMatrix) * a_normal);
    v_nWorld = nw;
    gl_Position = u_mvpMatrix * a_position;
}
