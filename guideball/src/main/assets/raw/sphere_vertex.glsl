uniform mat4 u_mvpMatrix;
uniform mat4 u_modelMatrix;
uniform mat4 u_viewMatrix;
attribute vec4 a_position;
attribute vec3 a_normal;
varying vec3 v_nWorld;
varying vec3 v_worldPos;
varying vec3 v_viewDir;

void main() {
    vec4 worldPos = u_modelMatrix * a_position;
    v_worldPos = worldPos.xyz;
    vec3 nw = normalize(mat3(u_modelMatrix) * a_normal);
    v_nWorld = nw;
    // 视线方向（从相机指向顶点）
    vec4 camPos = vec4(0.0, 0.0, 0.0, 1.0);
    v_viewDir = normalize(v_worldPos - camPos.xyz);
    gl_Position = u_mvpMatrix * a_position;
}
