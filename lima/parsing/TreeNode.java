package lima.parsing;
import java.util.ArrayList;
public class TreeNode {
	public double branchLength;
	public double score;	// bootstrap value, jackknife value, or posterior probabilities, etc.
	public String name;
	public Object tag;
	public TreeNode parent;
	public ArrayList<TreeNode> children;
	public TreeNode() {
		branchLength = score = -1;
		name = null;
		tag = null;
		parent = null;
		children = new ArrayList<TreeNode>();
	}
	public void addChild(TreeNode child) {
		children.add(child);
		child.parent = this;
	}
	public int leavesCount() {
		int count = 0;
		if(children.isEmpty()) count = 1;
		else for(TreeNode child : children) count += child.leavesCount();
		return count;
	}
	public int nameNodes(String prefix, int number) {
		if(name == null) {
			name = prefix + number;
			++number;
		};
		for(TreeNode child : children) {
			number = child.nameNodes(prefix, number);
		};
		return number;
	}
	public boolean isLeaf() {
		return children.isEmpty();
	}
	public void print(String tab) {
		String s = "";
		if(name != null) s += "\""+name+"\"";
		if(tag != null && tag instanceof String) s += " tag=\""+tag+"\"";
		if(score >= 0) s += " score=" + score;
		if(branchLength >= 0) s += " brlen=" + branchLength;
		System.out.println(tab + s);
		for(TreeNode child : children) {
			child.print(tab + "\t");
		};
	}
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if(!children.isEmpty()) {
			int childrenCount = children.size();
			sb.append("(");
			sb.append(children.get(0).toString());
			for(int i = 1; i < childrenCount; ++i) sb.append(",").append(children.get(i).toString());
			sb.append(")");
		};
		if(name != null) sb.append(name);
		else if(score >= 0) sb.append("" + score);
		if(branchLength >= 0) sb.append(":" + branchLength);
		return sb.toString();
	}
	public TreeNode copyStructure() {
		TreeNode node = new TreeNode();
		node.branchLength = branchLength;
		node.score = score;
		node.name = name;
		for(TreeNode child : children) {
			node.addChild(child.copyStructure());
		}
		return node;
	}
}