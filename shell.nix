{ pkgs ? import <nixpkgs> {} }:

let
  pythonEnv = pkgs.python3.withPackages (ps: with ps; [
    fastapi        # For building the Python notification server
    uvicorn        # ASGI server for running FastAPI applications
    httpx          # Asynchronous HTTP client for the webhook server
    requests        # For sending HTTP requests from the PC webhook server
  ]);
in
pkgs.mkShell {
  buildInputs = [
    pkgs.rustc
    pkgs.cargo
    pkgs.pkg-config
    pkgs.openssl
    pythonEnv
  ];

  # Optional: Set environment variables
  RUST_BACKTRACE = "full";

  # Optional: Shell hooks (commands that run when entering the shell)
  shellHook = ''
    echo "Welcome to the li CLI development environment!"
  '';
}
