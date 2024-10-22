#version 460

#extension GL_EXT_ray_query: enable

#include "light.glsl"
#include "fog.glsl"

layout (binding = 3, set = 0) uniform accelerationStructureEXT AS;

layout (binding = 2) uniform sampler2D Sampler0;

layout (binding = 1) uniform UBO {
    vec4 FogColor;
    float FogStart;
    float FogEnd;
    float AlphaCutout;
};

layout (binding = 0) uniform UniformBufferObject {
    mat4 MVP;
    mat4 ModelViewMat;
    mat4 ProjMat;
    vec2 ScreenSize;
};

layout (location = 0) in float vertexDistance;
layout (location = 1) in vec4 vertexColor;
layout (location = 2) in vec2 texCoord0;

layout (location = 0) out vec4 fragColor;


void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a < AlphaCutout) {
        discard;
    }

    vec2 pixel = vec2(gl_FragCoord.xy);
    vec2 size = normalize(vec2(ScreenSize.x, ScreenSize.y));
    if (any(greaterThanEqual(pixel, size)))
    {
        return;
    }
    vec2 px = vec2(pixel) + vec2(0.5);
    vec2 p = px / vec2(size);
    vec3 origin = vec3(
    ModelViewMat[0][3],
    ModelViewMat[1][3],
    ModelViewMat[2][3]
    );
    vec3 corners[4] = {
    (origin + vec3(0, 0, 1)) + vec3(origin.x / 2, origin.y / 2, 0),
    (origin + vec3(0, 0, 1)) + vec3(-origin.x / 2, origin.y / 2, 0),
    (origin + vec3(0, 0, 1)) + vec3(origin.x / 2, -origin.y / 2, 0),
    (origin + vec3(0, 0, 1)) + vec3(-origin.x / 2, -origin.y / 2, 0)
    };
    vec3 target = mix(mix(corners[0], corners[2], p.y), mix(corners[1], corners[3], p.y), p.x);
    vec4 direction = vec4(target - origin, 0.0);

    rayQueryEXT rayQuery;
    rayQueryInitializeEXT(
        rayQuery,
        AS,
        gl_RayFlagsOpaqueEXT,
        0xFF,
        origin,
        0.1,
        direction.xyz,
        100.0
    );
    while (rayQueryProceedEXT(rayQuery)) {}
    float t = rayQueryGetIntersectionTEXT(rayQuery, true);
    if (t < 100.0) {
        fragColor = color;
    } else
    {
        fragColor = normalize(color * vec4(t));
    }

    //    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
