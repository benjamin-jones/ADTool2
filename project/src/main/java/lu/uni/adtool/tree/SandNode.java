package lu.uni.adtool.tree;

import lu.uni.adtool.domains.SandDomain;
import lu.uni.adtool.domains.ValuationDomain;
import lu.uni.adtool.domains.rings.Ring;
import lu.uni.adtool.tools.Debug;
import lu.uni.adtool.tools.Options;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import bibliothek.util.xml.XAttribute;
import bibliothek.util.xml.XElement;

public class SandNode extends GuiNode {
  public SandNode() {
    super("Root");
    this.type = Type.AND;
  }

  public SandNode(Type type) {
    super();
    this.type = type;
  }

  /**
   * Type of node: and, or, sequential and, leaf node.
   */
  public enum Type {
    AND, OR, SAND
  }

  public static SandNode readStream(DataInputStream in) throws IOException {
    String name = in.readUTF();
    Type type = Type.values()[in.readInt()];
    SandNode result = new SandNode(type);
    result.setParent(null);
    result.setName(name);
    int noChildren = in.readInt();
    for (int i = 0; i < noChildren; i++) {
      SandNode child = readStream(in);
      result.addChild(child);
    }
    return result;
  }

  public void writeStream(DataOutputStream out) throws IOException {
    out.writeUTF(this.getName());
    out.writeInt(this.type.ordinal());
    if (getChildren() == null || getChildren().size() == 0) {
      out.writeInt(0);
    }
    else {
      out.writeInt(getChildren().size());
    }
    for (Node child : getChildren()) {
      ((SandNode) child).writeStream(out);
    }
  }
  /**
   * Import from XML using format that stores every tree together with layout used by Docking Frames
   *
   */

  public void fromXml(XElement e) {
    setName(e.getElement("label").getString());
    this.type = stringToType(e.getString("refinement"));
    for (XElement child : e.getElements("node")) {
      SandNode ch = new SandNode();
      ch.fromXml(child);
      this.addChild(ch);
    }
  }

  /**
   * Import from XML using format used by the first version of ADTool
   *
   */
  public ArrayList<ValuationDomain> importXml(XElement e, int treeId)
      throws IllegalArgumentException {
    XElement root;
    root = e.getElement("node");
    HashMap<String, ValuationDomain> domains = new HashMap<String, ValuationDomain>();
    int i = 0;
    for (XElement domain : e.getElements("domain")) {
      i++;
//       XElement etool = domain.getElement("tool");
//       if (etool == null) {
//         throw new IllegalArgumentException(Options.getMsg("exception.wrongtool"));
//       }
//       String tool = etool.getString();
//       if (tool == null || (!tool.startsWith("ADTool"))) {
//         throw new IllegalArgumentException(Options.getMsg("exception.wrongtool"));
//       }
      String domainName = domain.getElement("class").getString();
      SandDomain d = (SandDomain)DomainFactory.createFromString(domainName);// TODO - handling of domains with parameters
      if (d == null) {
        throw new IllegalArgumentException(Options.getMsg("exception.nodomain") + domainName);
      }
      String domainId = domain.getString("id");
      if (domainId == null || domains.containsKey(domainId)) {
        throw new IllegalArgumentException(Options.getMsg("exception.wrongxml"));
      }
      domains.put(domain.getString("id"), new ValuationDomain(treeId, i, d));
    }
    this.importXml(root, domains);
    return new ArrayList<ValuationDomain>(domains.values());
  }

  /**
   * Export to XML using format used by the first version of ADTool
   *
   */
  public XElement exportXml(ArrayList<ValuationDomain> domains) {
    XElement result = new XElement("node");
    result.addString("refinement", typeToXml(type));
    result.addElement("label").setString(getName());
    if (domains != null && Options.main_saveDomains) {
      for (int i = 0; i < domains.size(); i++) {
        ValuationDomain vd = domains.get(i);
        String domainId = vd.getExportXmlId();
        if (this.isEditable()) {
          if (vd.getValue(this) != null) {
            XElement param = result.addElement("parameter");
            param.addString("domainId", domainId);
            param.addString("category", "basic");
            param.setString(vd.getValue(this).toString());
          }
        }
        else {
          if (Options.main_saveDerivedValues) {
            XElement param = result.addElement("parameter");
            param.addString("domainId", domainId);
            param.addString("category", "derived");
            param.setString(vd.getTermValue(this).toString());
          }
        }
      }
    }
    if (this.getChildren() != null) {
      for (Node node : this.getNotNullChildren()) {
        result.addElement(((SandNode) node).exportXml(domains));
      }
    }
    return result;
  }
  /**
   * Export to XML using format to store every tree together with layout used by Docking Frames
   *
   */
  public XElement toXml() {
    XElement result = new XElement("node");
    XAttribute typeAttribute = new XAttribute("refinement");
    typeAttribute.setString(typeToString(type));
    result.addAttribute(typeAttribute);
    result.addElement("label").setString(getName());
    if (this.getChildren() != null) {
      for (Node node : this.getNotNullChildren()) {
        result.addElement(((SandNode) node).toXml());
      }
    }
    return result;
  }


  public String toString() {
    return "not implemented";
  }

  public Type getType() {
    return this.type;
  }

  public void setType(Type newType) {
    this.type = newType;
  }

  public String toTerms() {
    return toTerms(0);
  }

  public boolean isEditable() {
    return isLeaf();
  }

  public void toggleOp() {
    switch (type) {
    case AND:
      setType(Type.SAND);
      break;
    case SAND:
      setType(Type.OR);
      break;
    case OR:
      setType(Type.AND);
      break;
    }
  }

  /**
   * Pretty print a string representation of a tree.
   *
   * @param level
   * @return
   */
  public String toTerms(final int level) {
    String result = "";
    String indent = "";
    String currIndent = "";
    String eol = "";
    for (int i = 0; i < Options.indentLevel; i++) {
      indent += " ";
    }
    for (int i = 0; i < level; i++) {
      currIndent += indent;
    }
    eol = "\n";
    if (children != null && children.size() > 0) {
      for (int i = 0; i < children.size(); ++i) {
        final SandNode n = (SandNode) children.get(i);
        if (n != null) {
          result += n.toTerms(level + 1);
          if ((i + 1) < children.size()) {
            result += ",";
          }
          result += eol;
        }
        else {
          System.err.println("Null child at index:" + i);
        }
      }
      return currIndent + typeToString(type) +"(" + eol + result + currIndent + ")";
    }
    else {
      return currIndent + this.getName();
    }
  }

  private void importXml(XElement e, HashMap<String, ValuationDomain> domains)
      throws IllegalArgumentException {
    setName(e.getElement("label").getString());
    this.type = xmlToType(e.getString("refinement"));
    for (XElement parameter: e.getElements("parameter")) {
      String category = parameter.getString("category");
      if (category == null) {
        throw new IllegalArgumentException(Options.getMsg("exception.wrongxml"));
      }
      if (category.equals("basic")){
        ValuationDomain d = domains.get(parameter.getString("domainId"));
        if (d != null) {
          Ring r = d.getDomain().getDefaultValue(this);
          r.updateFromString(parameter.getString());
          d.setValue(getName(), r);
        }
      }
    }
    for (XElement child : e.getElements("node")) {
      SandNode ch = new SandNode();
      ch.importXml(child, domains);
      this.addChild(ch);
    }
  }

  private Type stringToType(String typeStr) {
    if (typeStr.equals("AND")) {
      return Type.AND;
    }
    else if (typeStr.equals("SAND")) {
      return Type.SAND;
    }
    else {
      return Type.OR;
    }
  }

  private String typeToString(Type type) {
    switch (this.type) {
    case AND:
      return "AND";
    case OR:
      return "OR";
    case SAND:
      return "SANd";
    }
    return "";
  }

  private Type xmlToType(String typeStr) {
    if (typeStr.equals("conjunctive")) {
      return Type.AND;
    }
    else if (typeStr.equals("disjunctive")) {
      return Type.OR;
    }
    else {
      return Type.SAND;
    }
  }

  private String typeToXml(Type type) {
    switch (this.type) {
    case AND:
      return "conjunctive";
    case OR:
      return "disjunctive";
    case SAND:
      return "sequential";
    }
    return "";
  }

  private Type type;

}