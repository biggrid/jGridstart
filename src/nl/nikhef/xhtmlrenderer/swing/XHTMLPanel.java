package nl.nikhef.xhtmlrenderer.swing;

import java.awt.Font;

import javax.swing.UIManager;

import org.w3c.dom.css.CSSPrimitiveValue;
import org.xhtmlrenderer.context.AWTFontResolver;
import org.xhtmlrenderer.css.constants.CSSName;
import org.xhtmlrenderer.css.extend.StylesheetFactory;
import org.xhtmlrenderer.css.newmatch.Selector;
import org.xhtmlrenderer.css.parser.CSSErrorHandler;
import org.xhtmlrenderer.css.parser.CSSParser;
import org.xhtmlrenderer.css.parser.PropertyValue;
import org.xhtmlrenderer.css.sheet.PropertyDeclaration;
import org.xhtmlrenderer.css.sheet.Ruleset;
import org.xhtmlrenderer.css.sheet.StylesheetInfo;
import org.xhtmlrenderer.css.style.CalculatedStyle;
import org.xhtmlrenderer.css.style.CssContext;
import org.xhtmlrenderer.css.style.derived.LengthValue;
import org.xhtmlrenderer.css.value.FontSpecification;
import org.xhtmlrenderer.extend.FontResolver;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.render.FSFont;
import org.xhtmlrenderer.simple.extend.XhtmlNamespaceHandler;

/** An {@link org.xhtmlrenderer.simple.XHTMLPanel} with minor tweaks.
 * <p>
 * This class sets the font from the system's default dialog font so
 * it blends nicely into the user-interface.
 * 
 * @author wvengen
 */
public class XHTMLPanel extends org.xhtmlrenderer.simple.XHTMLPanel {

    public XHTMLPanel() {
	super();
	initialize_tweaks();
    }

    public XHTMLPanel(UserAgentCallback uac) {
	super(uac);
	initialize_tweaks();
    }
    
    /** Setup the XHTMPanel tweaks */
    private void initialize_tweaks() {
	getSharedContext().setNamespaceHandler(new XhtmlNamespaceHandler() {
	    private StylesheetInfo _defaultStylesheet = null;
	    
	    @Override
	    public StylesheetInfo getDefaultStylesheet(StylesheetFactory factory) {
		if (_defaultStylesheet == null) {
		    // get default font properties
		    Font font = getFont();
		    // workaround Linux where it seems too large
		    String osName = System.getProperty("os.name");
		    if (!osName.startsWith("Mac OS") && !osName.startsWith("Windows"))
			font = font.deriveFont(font.getSize2D()*0.9f);
		    
		    // add to UA stylesheet
		    _defaultStylesheet = super.getDefaultStylesheet(factory);
		    
		    Ruleset fontRule = new Ruleset(StylesheetInfo.USER_AGENT);
		    PropertyValue family = new PropertyValue(PropertyValue.CSS_STRING, font.getName(), font.getName());
		    PropertyValue size = new PropertyValue(PropertyValue.CSS_PT, font.getSize2D(), font.getSize2D()+"pt");
		    fontRule.addProperty(new PropertyDeclaration(CSSName.FONT_FAMILY, family, false, StylesheetInfo.USER_AGENT));
		    fontRule.addProperty(new PropertyDeclaration(CSSName.FONT_SIZE, size, false, StylesheetInfo.USER_AGENT));
		    if (font.isBold()) {
			PropertyValue bold = new PropertyValue(PropertyValue.CSS_STRING, "bold", "bold");
			fontRule.addProperty(new PropertyDeclaration(CSSName.FONT_WEIGHT, bold, false, StylesheetInfo.USER_AGENT));
		    }
		    if (font.isItalic()) {
			PropertyValue italic = new PropertyValue(PropertyValue.CSS_STRING, "italic", "italic");
			fontRule.addProperty(new PropertyDeclaration(CSSName.FONT_STYLE, italic, false, StylesheetInfo.USER_AGENT));
		    }
		    
		    Selector bodySelector = new Selector();
		    bodySelector.setName("body");
		    bodySelector.setParent(fontRule);
		    fontRule.addFSSelector(bodySelector);
		    _defaultStylesheet.getStylesheet().addContent(fontRule);
		}
		return _defaultStylesheet;
	    }
	});
    }
    
}
