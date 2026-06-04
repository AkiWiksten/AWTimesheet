package com.akiwiksten.awtimesheet.feature.timesheet.workbook.xml

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

internal fun replaceSharedStrings(
    sharedStringsXml: ByteArray,
    replacements: Map<Int, String>
): ByteArray {
    val document = createDocumentBuilderFactory().newDocumentBuilder()
        .parse(ByteArrayInputStream(sharedStringsXml))
    val sharedStrings = document.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "si")

    for ((index, value) in replacements) {
        val sharedString = sharedStrings.item(index) as? Element
        if (sharedString != null) {
            val textNodes = sharedString.getElementsByTagNameNS(SPREADSHEET_NAMESPACE, "t")
            updateSharedStringTextNodes(textNodes, value)
        }
    }

    return document.toByteArray()
}

private fun updateSharedStringTextNodes(textNodes: NodeList, value: String) {
    val preserveXmlSpace = value.firstOrNull()?.isWhitespace() == true ||
        value.lastOrNull()?.isWhitespace() == true

    for (i in 0 until textNodes.length) {
        val textNode = textNodes.item(i) as? Element
        if (textNode != null) {
            textNode.textContent = value
            updateXmlSpaceAttribute(textNode, preserveXmlSpace)
        }
    }
}

private fun updateXmlSpaceAttribute(textNode: Element, preserveXmlSpace: Boolean) {
    if (preserveXmlSpace) {
        textNode.setAttributeNS(XML_NAMESPACE, "xml:space", "preserve")
    } else {
        textNode.removeAttributeNS(XML_NAMESPACE, "space")
    }
}

internal fun createDocumentBuilderFactory(): DocumentBuilderFactory {
    return DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
    }
}

internal fun Document.toByteArray(): ByteArray {
    return ByteArrayOutputStream().use { output ->
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
        }
        transformer.transform(DOMSource(this), StreamResult(output))
        output.toByteArray()
    }
}

internal fun Element.childElementSequence(localName: String): Sequence<Element> = sequence {
    var child = firstChild
    while (child != null) {
        if (child.nodeType == Node.ELEMENT_NODE && child.localName == localName) {
            yield(child as Element)
        }
        child = child.nextSibling
    }
}

internal const val SPREADSHEET_NAMESPACE = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
internal const val XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace"
internal const val CONTENT_TYPES_NAMESPACE = "http://schemas.openxmlformats.org/package/2006/content-types"
internal const val RELATIONSHIPS_NAMESPACE = "http://schemas.openxmlformats.org/package/2006/relationships"
