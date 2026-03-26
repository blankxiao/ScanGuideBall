precision mediump float;
uniform int u_gridCols;
uniform int u_gridRows;
uniform float u_dotRadius;
varying vec3 v_nWorld;
varying vec3 v_worldPos;
varying vec3 v_viewDir;

// 将球面坐标转换为 3D 单位向量
vec3 sphericalToCartesian(float lon, float lat) {
    float cosLat = cos(lat);
    return vec3(
        cosLat * sin(lon),
        sin(lat),
        cosLat * cos(lon)
    );
}

// 找到最近的网格点并返回其在球面上的法线
vec3 findNearestGridPoint(vec3 normal, int cols, int rows) {
    // 从法线计算经纬度
    float lon = atan(normal.x, normal.z);
    float lat = asin(clamp(normal.y, -1.0, 1.0));

    // 计算网格步长
    float lonStep = 2.0 * 3.14159265 / float(cols);
    float latStep = 3.14159265 / float(rows + 1);

    // 纬度起始偏移（避开两极）
    float latStart = -0.5 * 3.14159265 + latStep;

    // 找到最近的网格点
    float minAngleDist = 1000.0;
    vec3 nearestPoint = vec3(0.0, 1.0, 0.0);

    // 只在纬度范围内搜索
    for (int row = 0; row < 20; row++) {
        if (row >= rows) break;

        float gridLat = latStart + float(row) * latStep;
        float cosLat = cos(gridLat);
        if (cosLat < 0.01) continue;

        for (int col = 0; col < 20; col++) {
            if (col >= cols) break;

            float gridLon = float(col) * lonStep;
            vec3 gridPoint = sphericalToCartesian(gridLon, gridLat);

            float angleDist = acos(clamp(dot(normal, gridPoint), -1.0, 1.0));
            if (angleDist < minAngleDist) {
                minAngleDist = angleDist;
                nearestPoint = gridPoint;
            }
        }
    }

    return nearestPoint;
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

    // 找到最近的网格点
    vec3 nearestGrid = findNearestGridPoint(nw, u_gridCols, u_gridRows);

    // 使用切线空间距离计算圆形（屏幕一致的圆）
    float tangentDist = calcTangentPlaneDistance(v_worldPos, nearestGrid, u_dotRadius);

    // 绘制圆点
    if (tangentDist < u_dotRadius) {
        float t = tangentDist / u_dotRadius;
        // 锐利边缘的圆
        brightness = pow(1.0 - t, 2.0) * 0.65;

        // 中心高光
        if (t < 0.4) {
            brightness += 0.25 * (1.0 - t / 0.4);
        }
    }

    float d = clamp(base + brightness, 0.0, 1.0);
    gl_FragColor = vec4(vec3(d), 1.0);
}
