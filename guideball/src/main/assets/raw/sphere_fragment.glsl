precision mediump float;
uniform int u_gridCols;
uniform int u_gridRows;
uniform float u_dotRadius;
uniform float u_collectedMask[512];
uniform int u_collectingIndex;
uniform float u_collectProgress;
varying vec3 v_nWorld;
varying vec3 v_worldPos;
varying vec3 v_viewDir;

const float PI = 3.14159265;
const float TWO_PI = 6.28318530;

// 将球面坐标转换为 3D 单位向量
vec3 sphericalToCartesian(float lon, float lat) {
    float cosLat = cos(lat);
    return vec3(
        cosLat * sin(lon),
        sin(lat),
        cosLat * cos(lon)
    );
}

// O(1) 量化到最近网格点，避免每像素遍历全网格
void quantizeNearestGridPoint(vec3 normal, int cols, int rows, out vec3 nearestPoint, out int nearestIndex) {
    float lon = atan(normal.x, normal.z);
    if (lon < 0.0) lon += TWO_PI;
    float lat = asin(clamp(normal.y, -1.0, 1.0));

    float lonStep = TWO_PI / float(cols);
    float latStep = PI / float(rows + 1);
    float latStart = -0.5 * PI + latStep;

    float rowF = floor((lat - latStart) / latStep + 0.5);
    int row = int(clamp(rowF, 0.0, float(rows - 1)));

    float colF = floor(lon / lonStep + 0.5);
    int col = int(mod(colF, float(cols)));
    if (col < 0) col += cols;

    float gridLat = latStart + float(row) * latStep;
    float gridLon = float(col) * lonStep;
    nearestPoint = sphericalToCartesian(gridLon, gridLat);
    nearestIndex = row * cols + col;
}

// 计算在网格点切平面上的投影距离（屏幕空间一致的圆）
float calcTangentPlaneDistance(vec3 worldPos, vec3 gridNormal, float radius) {
    // 网格点位置（球面上半径=1）
    vec3 gridPos = gridNormal;

    // 从网格点指向当前片元的向量
    vec3 toFragment = worldPos - gridPos;

    // 将向量投影到切平面（减去沿法线的分量）
    vec3 tangentProj = toFragment - dot(toFragment, gridNormal) * gridNormal;

    // 计算切平面上的距离（弧度制，与radius单位一致）
    float dist = length(tangentProj);

    return dist;
}

void main() {
    vec3 nw = normalize(v_nWorld);

    // 基础底色
    float base = 0.55;
    float brightness = 0.0;

    // 直接量化到最近网格点
    vec3 nearestGrid;
    int nearestIndex;
    quantizeNearestGridPoint(nw, u_gridCols, u_gridRows, nearestGrid, nearestIndex);

    bool collected = false;
    if (nearestIndex >= 0 && nearestIndex < 512) {
        collected = u_collectedMask[nearestIndex] > 0.5;
    }

    bool collecting = nearestIndex == u_collectingIndex;
    float dynamicDotRadius = u_dotRadius;
    if (collecting) {
        float easedProgress = smoothstep(0.0, 1.0, u_collectProgress);
        dynamicDotRadius = mix(u_dotRadius, u_dotRadius * 2.4, easedProgress);
    }

    // 使用切线空间距离计算圆形（屏幕一致的圆）
    float tangentDist = calcTangentPlaneDistance(v_worldPos, nearestGrid, dynamicDotRadius);

    // 绘制圆点
    if (tangentDist < dynamicDotRadius) {
        float t = tangentDist / dynamicDotRadius;
        // 锐利边缘的圆
        brightness = pow(1.0 - t, 2.0) * 0.65;

        // 中心高光
        if (t < 0.4) {
            brightness += 0.25 * (1.0 - t / 0.4);
        }
        if (collecting) {
            float pulse = smoothstep(0.0, 1.0, u_collectProgress);
            brightness += 0.35 * pulse;
        }
        if (collected) {
            brightness += 0.42;
        }
    }

    float d = clamp(base + brightness, 0.0, 1.0);
    gl_FragColor = vec4(vec3(d), 1.0);
}
