import xml.etree.ElementTree as ET

def list_viewport_children(xml_path):
    tree = ET.parse(xml_path)
    root = tree.getroot()
    
    def find_viewport(node):
        bounds = node.get("bounds", "")
        # The viewport box bounds we found are '[37,825][1043,1714]'
        if bounds == "[37,825][1043,1714]":
            return node
        for child in node:
            res = find_viewport(child)
            if res is not None:
                return res
        return None
        
    viewport_node = find_viewport(root)
    if viewport_node is not None:
        print("Viewport Box Children:")
        def print_children(node, depth=0):
            text = node.get("text", "").encode("ascii", "replace").decode("ascii")
            cls = node.get("class", "")
            bounds = node.get("bounds", "")
            print(f"{'  ' * depth}- CLASS='{cls}' TEXT='{text}' BOUNDS='{bounds}'")
            for child in node:
                print_children(child, depth + 1)
        print_children(viewport_node)
    else:
        print("Viewport box not found by exact bounds")

if __name__ == "__main__":
    list_viewport_children(r"f:\floraflow-garden-designer\window_dump_current.xml")
