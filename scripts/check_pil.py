try:
    import PIL
    from PIL import Image, ImageDraw
    print("PIL is available")
except ImportError:
    print("PIL is NOT available")
