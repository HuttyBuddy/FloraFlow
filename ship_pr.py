import sys
import subprocess

def main():
    title = "🧹 [Code Health] Remove unused Text import in PlantImages.kt"
    with open('pr_message.txt', 'r') as f:
        description = f.read()

    print(f"Shipping PR:\nTitle: {title}\n\nDescription:\n{description}")

if __name__ == "__main__":
    main()
