#version 460

#extension GL_EXT_ray_query: enable
#extension GL_EXT_ray_flags_primitive_culling: enable

#include "light.glsl"
#include "fog.glsl"

layout (binding = 5, set = 0) uniform accelerationStructureEXT AS;

layout (binding = 2) uniform sampler2D Sampler0;

layout (binding = 1) uniform UBO {
    vec4 FogColor;
    float FogStart;
    float FogEnd;
    float AlphaCutout;
    vec3 cameraPos;
    vec2 ScreenSize;
    mat4 ModelViewMat;
    vec3 Light1_Direction;
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
    vec2 size = vec2(ScreenSize.x, ScreenSize.y);
    if (any(greaterThanEqual(pixel, size)))
    {
        discard;
    }
    vec2 px = vec2(pixel) + vec2(0.5);
    vec2 p = px / vec2(size);

    vec3 origin = cameraPos;
    vec3 corners[4] = {
    vec3(origin.x / 2, origin.y / 2, 1.0),
    vec3(-origin.x / 2, origin.y / 2, 1.0),
    vec3(origin.x / 2, -origin.y / 2, 1.0),
    vec3(-origin.x / 2, -origin.y / 2, 1.0)
    };
    vec3 target = mix(mix(corners[0], corners[2], p.y), mix(corners[1], corners[3], p.y), p.x);
    vec4 direction = ModelViewMat * vec4(normalize(target.xyz), 0.0);

    rayQueryEXT rayQuery;
    rayQueryInitializeEXT(
        rayQuery,
        AS,
        gl_RayFlagsTerminateOnFirstHitEXT | gl_RayFlagsCullNoOpaqueEXT | gl_RayFlagsSkipAABBEXT,
        0xFF,
        origin,
        0.1,
        direction.xyz,
        500.0
    );
    while (rayQueryProceedEXT(rayQuery)) {}
    float t = rayQueryGetIntersectionTEXT(rayQuery, true);

    if (t > 0.0) {
        fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    } else
    {
        fragColor = linear_fog(color * vec4(0.1, 0.1, 0.1, 1.0), vertexDistance, FogStart, FogEnd, FogColor);
    }

//        fragColor = normalize(vec4(t, t, t, 1.0));

    //    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
