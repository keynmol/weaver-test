package com.disneystreaming
package weaver.docs

import subatomic._
import subatomic.Discover.MarkdownDocument

object Build extends App {
  case class WeaverPage(
      title: String,
      markdownFile: os.Path,
      overrideWeaverVersion: Option[String]
  )

  val content = Discover.someMarkdown(os.pwd) {
    case MarkdownDocument(path, _, attributes) =>
      val id = attributes.requiredOne("id")

      // docusaurus style, creating folders with a single index.html
      val sitePath = SiteRoot / id / "index.html" 

      sitePath -> WeaverPage(attributes.requiredOne("title"),
                             path,
                             None // use current version
      ) 
  }

  val defaultMdoc =
    new Mdoc(inheritClasspath = true, variables = Map("VERSION" -> "0.6.0-M1"))

  val defaultMdocProcessor = MdocProcessor.create[WeaverPage](defaultMdoc) {
    case WeaverPage(_, path, _) => MdocFile(path, dependencies = Set())
  }

  val markdown = Markdown()

  def renderMarkdownFile(path: os.Path): SiteAsset = {
    Page(markdown.renderToString(path))
  }

  val mdocPageRenderer = defaultMdocProcessor.map { result =>
    renderMarkdownFile(result.resultFile)
  }

  Site
    .init(content)
    .populate { case (site, (sitePath, content)) =>
      content match {
        case cont: WeaverPage =>
          site.addProcessed(sitePath, mdocPageRenderer, cont)
      }
    }
    .buildAt(os.pwd / "site")

}
