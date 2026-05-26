{
  description = "Development environment for a scheduled Java API ingester";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      systems = [
        "x86_64-linux"
        "aarch64-linux"
        "x86_64-darwin"
        "aarch64-darwin"
      ];

      forEachSystem = f:
        nixpkgs.lib.genAttrs systems (system:
          f {
            pkgs = import nixpkgs {
              inherit system;
            };
          });
    in
    {
      formatter = forEachSystem ({ pkgs }: pkgs.nixpkgs-fmt);

      devShells = forEachSystem ({ pkgs }: {
        default = pkgs.mkShell {
          packages = with pkgs; [
            eza
            bat
            starship
            jdk21
            gradle
            mysql84
            curl
            jq
            git
            nil
            nixpkgs-fmt
          ];

          shellHook = ''
            alias l="eza -hl --hyperlink --header --icons --octal-permissions -a"
            alias tree="eza -hl --git --git-repos -T --hyperlink --header --icons --octal-permissions --git-ignore -L=2 -a"
            alias bat="bat --theme='Catppuccin Frappe'"
            alias sail="[ -f sail ] && sh sail || sh vendor/bin/sail"
            export JAVA_HOME="${pkgs.jdk21}"
            export GRADLE_OPTS="-Dorg.gradle.daemon=false"

            echo "Java development shell ready"
            echo "  Java:    $(java -version 2>&1 | head -n 1)"
            echo "  Gradle:  $(gradle --version | sed -n 's/^Gradle //p' | head -n 1)"
            echo "  MySQL:   $(mysql --version)"
          '';
        };
      });
    };
}
