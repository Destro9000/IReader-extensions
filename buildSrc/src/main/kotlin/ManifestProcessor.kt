import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import com.android.build.gradle.tasks.ManifestProcessorTask
import com.android.build.gradle.tasks.ProcessMultiApkApplicationManifest
import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlNodePrinter
import groovy.xml.XmlParser
import org.gradle.api.GradleException
import java.io.File

fun BaseVariantOutputImpl.processManifestForExtension(extension: Extension) {
  processManifestProvider.configure { 
    doLast { 
      processExtension(extension)
    }
  }
}

private fun ManifestProcessorTask.processExtension(extension: Extension) {
  this as ProcessMultiApkApplicationManifest
  val outputDirectory = multiApkManifestOutputDirectory.get().asFile
  val manifestFile = File(outputDirectory, "AndroidManifest.xml")

  if (!manifestFile.exists()) {
    throw GradleException("Can't find manifest file for ${extension.name}")
  }

  val extClass = "tachiyomix.extension.Extension" // This class is generated by ksp
  val parser = XmlParser().parse(manifestFile)

  // Replace the package name
  parser.attributes()["package"] = extension.applicationId

  val app = (parser["application"] as NodeList).first() as Node

  // Add source class metadata
  Node(app, "meta-data", mapOf(
    "android:name" to "source.class",
    "android:value" to extClass
  ))

  // Add deeplinks if needed
  addDeepLinks(app, extension.deepLinks)

  XmlNodePrinter(manifestFile.printWriter()).print(parser)
}

private fun addDeepLinks(app: Node, deeplinks: List<DeepLink>) {
  if (deeplinks.isEmpty()) return

  val activity = Node(app, "activity", mapOf(
    "android:name" to "tachiyomix.deeplink.SourceDeepLinkActivity",
    "android:exported" to "true",
    "android:theme" to "@android:style/Theme.NoDisplay"
  ))

  val filter = Node(activity, "intent-filter")
  Node(filter, "action", mapOf("android:name" to "android.intent.action.VIEW"))
  Node(filter, "category", mapOf("android:name" to "android.intent.category.DEFAULT"))
  Node(filter, "category", mapOf("android:name" to "android.intent.category.BROWSABLE"))

  deeplinks.forEach { link ->
    val data = mutableMapOf<String, String>()
    if (link.scheme.isNotEmpty()) {
      data["android:scheme"] = link.scheme
    }
    if (link.host.isNotEmpty()) {
      data["android:host"] = link.host
    }
    if (link.pathPattern.isNotEmpty()) {
      data["android:pathPattern"] = link.pathPattern
    }
    if (link.path.isNotEmpty()) {
      data["android:path"] = link.path
    }

    Node(filter, "data", data)
  }
}
