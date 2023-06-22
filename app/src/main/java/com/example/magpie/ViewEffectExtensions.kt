package com.example.magpie

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.view.View

sealed class Effect {
    object None : Effect()
    object BoxBlur : Effect()
    object Pixelize : Effect()
    object Acrylic : Effect()
}

fun View.applyEffect(effect: Effect) {
    when (effect) {
        Effect.None -> this.clearEffect()
        Effect.BoxBlur -> this.applyBoxBlur()
        Effect.Pixelize -> this.applyPixelizeEffect()
        Effect.Acrylic -> this.applyAcrylicEffect()
    }
}

private fun View.clearEffect() {
    this.setRenderEffect(null)
}

private fun View.applyBoxBlur() {
    val shaderSrc = """
            uniform shader uViewSrc; 
            half4 getAverageColor(float2 fragCoord) {
                half4 res = half4(0.0);
                for (int x = -16; x <= 16; x++) {
                    for (int y = -16; y <= 16; y++) {
                        float2 coord = fragCoord + float2(x,y);
                        res += uViewSrc.eval(coord);
                    }
                }
                return res / 1089.0;
            }
            
            half4 main(float2 fragCoord) {
                return getAverageColor(fragCoord);
            }
        """
    val shader = RuntimeShader(shaderSrc)
    this.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "uViewSrc"))
}

private fun View.applyPixelizeEffect() {
    val shaderSrc = """
            uniform shader uViewSrc; 
            uniform float2 uResolution;
            const float size = 64.0;
            
            half4 main(float2 fragCoord) {
                float2 newCoord = floor(fragCoord / size) * size;
                return uViewSrc.eval(newCoord); 
            }
        """
    val shader = RuntimeShader(shaderSrc)
    this.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "uViewSrc"))
}

/**
 * https://stegu.github.io/webgl-noise/webdemo/
 */
private fun View.applyAcrylicEffect() {
    val noiseShaderSrc = """
        uniform shader uViewSrc;
        uniform float2 uResolution;
       
        //
        // Description : Array and textureless GLSL 2D/3D/4D simplex 
        //               noise functions.
        //      Author : Ian McEwan, Ashima Arts.
        //  Maintainer : stegu
        //     Lastmod : 20201014 (stegu)
        //     License : Copyright (C) 2011 Ashima Arts. All rights reserved.
        //               Distributed under the MIT License. See LICENSE file.
        //               https://github.com/ashima/webgl-noise
        //               https://github.com/stegu/webgl-noise
        // 
        
        vec3 mod289(vec3 x) {
          return x - floor(x * (1.0 / 289.0)) * 289.0;
        }
        
        vec4 mod289(vec4 x) {
          return x - floor(x * (1.0 / 289.0)) * 289.0;
        }
        
        vec4 permute(vec4 x) {
             return mod289(((x*34.0)+10.0)*x);
        }
        
        vec4 taylorInvSqrt(vec4 r)
        {
          return 1.79284291400159 - 0.85373472095314 * r;
        }
        
        float snoise(vec3 v)
          { 
          const vec2  C = vec2(1.0/6.0, 1.0/3.0) ;
          const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);
        
        // First corner
          vec3 i  = floor(v + dot(v, C.yyy) );
          vec3 x0 =   v - i + dot(i, C.xxx) ;
        
        // Other corners
          vec3 g = step(x0.yzx, x0.xyz);
          vec3 l = 1.0 - g;
          vec3 i1 = min( g.xyz, l.zxy );
          vec3 i2 = max( g.xyz, l.zxy );
        
          //   x0 = x0 - 0.0 + 0.0 * C.xxx;
          //   x1 = x0 - i1  + 1.0 * C.xxx;
          //   x2 = x0 - i2  + 2.0 * C.xxx;
          //   x3 = x0 - 1.0 + 3.0 * C.xxx;
          vec3 x1 = x0 - i1 + C.xxx;
          vec3 x2 = x0 - i2 + C.yyy; // 2.0*C.x = 1/3 = C.y
          vec3 x3 = x0 - D.yyy;      // -1.0+3.0*C.x = -0.5 = -D.y
        
        // Permutations
          i = mod289(i); 
          vec4 p = permute( permute( permute( 
                     i.z + vec4(0.0, i1.z, i2.z, 1.0 ))
                   + i.y + vec4(0.0, i1.y, i2.y, 1.0 )) 
                   + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));
        
        // Gradients: 7x7 points over a square, mapped onto an octahedron.
        // The ring size 17*17 = 289 is close to a multiple of 49 (49*6 = 294)
          float n_ = 0.142857142857; // 1.0/7.0
          vec3  ns = n_ * D.wyz - D.xzx;
        
          vec4 j = p - 49.0 * floor(p * ns.z * ns.z);  //  mod(p,7*7)
        
          vec4 x_ = floor(j * ns.z);
          vec4 y_ = floor(j - 7.0 * x_ );    // mod(j,N)
        
          vec4 x = x_ *ns.x + ns.yyyy;
          vec4 y = y_ *ns.x + ns.yyyy;
          vec4 h = 1.0 - abs(x) - abs(y);
        
          vec4 b0 = vec4( x.xy, y.xy );
          vec4 b1 = vec4( x.zw, y.zw );
        
          //vec4 s0 = vec4(lessThan(b0,0.0))*2.0 - 1.0;
          //vec4 s1 = vec4(lessThan(b1,0.0))*2.0 - 1.0;
          vec4 s0 = floor(b0)*2.0 + 1.0;
          vec4 s1 = floor(b1)*2.0 + 1.0;
          vec4 sh = -step(h, vec4(0.0));
        
          vec4 a0 = b0.xzyw + s0.xzyw*sh.xxyy ;
          vec4 a1 = b1.xzyw + s1.xzyw*sh.zzww ;
        
          vec3 p0 = vec3(a0.xy,h.x);
          vec3 p1 = vec3(a0.zw,h.y);
          vec3 p2 = vec3(a1.xy,h.z);
          vec3 p3 = vec3(a1.zw,h.w);
        
        //Normalise gradients
          vec4 norm = taylorInvSqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
          p0 *= norm.x;
          p1 *= norm.y;
          p2 *= norm.z;
          p3 *= norm.w;
        
        // Mix final noise value
          vec4 m = max(0.5 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
          m = m * m;
          return 105.0 * dot( m*m, vec4( dot(p0,x0), dot(p1,x1), 
                                        dot(p2,x2), dot(p3,x3) ) );
          }
        
        // demo code:
        float color(vec2 xy) { return 0.7 * snoise(vec3(xy, 1.0)); }
        
        half4 main(float2 fragCoord) {
            vec2 p = (fragCoord.xy/uResolution.y) * 2.0 - 1.0;
        
            vec3 xyz = vec3(p, 0);
        
            vec2 step = vec2(1.3, 1.7);
            float n = color(xyz.xy);
            n += 0.5 * color(xyz.xy * 2.0 - step);
            n += 0.25 * color(xyz.xy * 4.0 - 2.0 * step);
            n += 0.125 * color(xyz.xy * 8.0 - 3.0 * step);
            n += 0.0625 * color(xyz.xy * 16.0 - 4.0 * step);
            n += 0.03125 * color(xyz.xy * 32.0 - 5.0 * step);

            // hanstest
            vec2 normCoord = (fragCoord.xy/uResolution.xy);
            float distanceToCenter = distance(vec2(0.5), normCoord);
            
            // radial gradient from white->black (in->out)
            half3 color = 1.0 - half3(distanceToCenter);
            // mix radial gradient with noise
            half3 noiseValue = vec3(0.5 + 0.5 * vec3(n, n, n));
            color = mix(color, noiseValue, 0.25);
            // mix result with the source image color
            color = mix(color, uViewSrc.eval(fragCoord).rgb, 0.5);
            // looks... alright
            return half4(color, 1.0);
        }
       """
    val noiseShader = RuntimeShader(noiseShaderSrc)
    noiseShader.setFloatUniform("uResolution", this.width.toFloat(), this.height.toFloat())

    val blurEffect = RenderEffect.createBlurEffect(32f, 32f, Shader.TileMode.CLAMP)
    val noiseEffect = RenderEffect.createRuntimeShaderEffect(noiseShader, "uViewSrc");

    this.setRenderEffect(RenderEffect.createChainEffect(noiseEffect, blurEffect))
}