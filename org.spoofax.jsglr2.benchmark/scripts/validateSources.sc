import $ivy.`com.lihaoyi::ammonite-ops:1.8.1`, ammonite.ops._

import $file.args, args._, Args._
import $file.config, config.config

import $file.spoofax, spoofax._
import org.spoofax.jsglr2.JSGLR2Variant
import org.spoofax.jsglr2.integration.IntegrationVariant
import org.spoofax.jsglr2.integration.ParseTableVariant

def validateSources(implicit args: Args) = {
    println("Validating sources...")

    config.languages.foreach { language =>
        println(" " + language.id)

        val files = ls.rec! language.sourcesDir

        val variant = new IntegrationVariant(
            new ParseTableVariant(),
            JSGLR2Variant.Preset.standard.variant
        )

        val jsglr2 = getJSGLR2(variant, language.parseTablePath)

        files.foreach { file =>
            val ast = jsglr2.parse(read! file)

            if (ast == null) {
                val filename = file relativeTo language.sourcesDir

                println("   Invalid: " + filename)

                mkdir! language.sourcesDir / "invalid"
                mv(file, language.sourcesDir / "invalid" / filename)
            }
        }
    }
}

@main
def ini(args: String*) = withArgs(args :_ *)(validateSources(_))