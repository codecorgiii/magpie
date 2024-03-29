package com.example.magpie

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.view.View
import androidx.annotation.FloatRange

// TODO: The other filters in https://developer.mozilla.org/en-US/docs/Web/CSS/filter
sealed class Filter {
    object None : Filter()
    object Greyscale : Filter()
    data class Sepia(@FloatRange(from = 0.0, to = 1.0) val amount: Float = 1f) : Filter()
    object Invert : Filter()
    data class HueRotate(@FloatRange(from = -360.0, to = 360.0) val degrees: Float = 0f) :
        Filter()
}

fun View.applyFilter(filter: Filter) {
    when (filter) {
        Filter.Greyscale -> this.applyGreyscaleFilter()
        is Filter.HueRotate -> this.applyHueRotationFilter(filter.degrees)
        Filter.Invert -> this.applyInvertFilter()
        Filter.None -> this.clearFilter()
        is Filter.Sepia -> this.applySepiaFilter(filter.amount)
    }
}

private fun View.applyInvertFilter() {
    val shaderSrc = """
            uniform shader viewSrc;
            half4 main(float2 fragCoord) {
                half4 fragSrc = viewSrc.eval(fragCoord);
                fragSrc.rgb = half3(1.0) - fragSrc.rgb;
                return fragSrc;
            }
        """
    val shader = RuntimeShader(shaderSrc)
    this.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "viewSrc"))
}

private fun View.clearFilter() {
    this.setRenderEffect(null)
}

/**
 * https://www.w3.org/TR/filter-effects-1/#sepiaEquivalent
 */
private fun View.applySepiaFilter(@FloatRange(from = 0.0, to = 1.0) amount: Float) {
    val shaderSrc = """
            uniform shader viewSrc;
            uniform float uAmount;
            
            half4 main(float2 fragCoord) {
                half4 fragSrc = viewSrc.eval(fragCoord);
                half3x3 sepiaMatrix;
                sepiaMatrix[0][0] = (0.393 + 0.607 * (1 - uAmount));
                sepiaMatrix[0][1] = (0.769 - 0.769 * (1 - uAmount));
                sepiaMatrix[0][2] = (0.189 - 0.189 * (1 - uAmount));
                sepiaMatrix[1][0] = (0.349 - 0.349 * (1 - uAmount));
                sepiaMatrix[1][1] = (0.686 + 0.314 * (1 - uAmount));
                sepiaMatrix[1][2] = (0.168 - 0.168 * (1 - uAmount));
                sepiaMatrix[2][0] = (0.272 - 0.272 * (1 - uAmount));
                sepiaMatrix[2][1] = (0.534 - 0.534 * (1 - uAmount));
                sepiaMatrix[2][2] = (0.131 + 0.869 * (1 - uAmount));
                // TODO: Should the multiplication be done with linearRGB color instead?
                half3 outColor = fragSrc.rgb * sepiaMatrix;
                return half4(outColor, fragSrc.a);
            }
        """
    val shader = RuntimeShader(shaderSrc).apply {
        setFloatUniform("uAmount", amount)
    }
    this.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "viewSrc"))
}

/**
 * https://en.wikipedia.org/wiki/Relative_luminance
 * https://en.wikipedia.org/wiki/Grayscale
 * https://developer.android.com/develop/ui/views/graphics/agsl/agsl-quick-reference#color_functions
 */
private fun View.applyGreyscaleFilter() {
    val shaderSrc = """
            uniform shader viewSrc;
            half4 main(float2 fragCoord) {
                half4 fragSrc = viewSrc.eval(fragCoord);
                half3 fragColorLinear = toLinearSrgb(fragSrc.rgb);
                half relativeLuminance = dot(half3(0.2126, 0.7152, 0.0722), fragColorLinear);
                // TODO: Not sure why I have to convert back to non-linear sRGB to make it look nicer?
                return half4(fromLinearSrgb(half3(relativeLuminance)), fragSrc.a);
            }
        """
    val shader = RuntimeShader(shaderSrc)
    this.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "viewSrc"))
}

/**
 * https://chilliant.com/rgb2hsv.html
 * https://thebookofshaders.com/06/
 */
private fun View.applyHueRotationFilter(@FloatRange(from = -360.0, to = 360.0) degrees: Float) {
    val shaderSrc = """
            uniform shader uViewSrc;
            uniform float uDegrees;
            
            float normalize(float min, float max, float value) {
                return (value - min) / (max - min);
            }
            
            vec3 rgb2hsb( vec3 c ){
                vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
                vec4 p = mix(vec4(c.bg, K.wz),
                             vec4(c.gb, K.xy),
                             step(c.b, c.g));
                vec4 q = mix(vec4(p.xyw, c.r),
                             vec4(c.r, p.yzx),
                             step(p.x, c.r));
                float d = q.x - min(q.w, q.y);
                float e = 1.0e-10;
                return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)),
                            d / (q.x + e),
                            q.x);
            }
            
            //  Function from Iñigo Quiles
            //  https://www.shadertoy.com/view/MsS3Wc
            vec3 hsb2rgb( vec3 c ){
                vec3 rgb = clamp(abs(mod(c.x*6.0+vec3(0.0,4.0,2.0),
                                         6.0)-3.0)-1.0,
                                 0.0,
                                 1.0 );
                rgb = rgb*rgb*(3.0-2.0*rgb);
                return c.z * mix(vec3(1.0), rgb, c.y);
            }

            half4 main(float2 fragCoord) {
                vec4 fragSrc = uViewSrc.eval(fragCoord);
                vec3 fragColorHsb = rgb2hsb(fragSrc.rgb);
                // x here is the normalized hue component
                fragColorHsb.x = fragColorHsb.x + (mod(normalize(0.0, 360.0, uDegrees), 2.0) - 1.0);
                return half4(hsb2rgb(fragColorHsb), 1.0);
            }
        """
    val shader = RuntimeShader(shaderSrc).apply {
        setFloatUniform("uDegrees", degrees)
    }
    this.setRenderEffect(RenderEffect.createRuntimeShaderEffect(shader, "uViewSrc"))
}