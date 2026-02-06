#!/bin/bash
#
# ChemAudit Deployment Script
# Usage: ./deploy.sh [profile]
#
# Profiles: small, medium (default), large, xl, coconut
#

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_DIR="${SCRIPT_DIR}/config"

# Available profiles
PROFILES=("small" "medium" "large" "xl" "coconut")

# Function to print colored output
print_header() {
    echo -e "\n${BLUE}${BOLD}╔════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}${BOLD}║${NC}          ${CYAN}${BOLD}ChemAudit Deployment Script${NC}              ${BLUE}${BOLD}║${NC}"
    echo -e "${BLUE}${BOLD}╚════════════════════════════════════════════════════╝${NC}\n"
}

print_profile_info() {
    local profile=$1
    echo -e "${BOLD}Profile: ${CYAN}${profile}${NC}"
    echo -e "────────────────────────────────────"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}!${NC} $1"
}

# Function to display profile selection menu
show_menu() {
    echo -e "${BOLD}Select a deployment profile:${NC}\n"
    echo -e "  ${CYAN}1)${NC} small    - Dev/light use (1K molecules, 2+1 workers)"
    echo -e "  ${CYAN}2)${NC} medium   - Moderate workloads (10K molecules, 4+2 workers) ${GREEN}[default]${NC}"
    echo -e "  ${CYAN}3)${NC} large    - High-throughput (50K molecules, 8+2 workers)"
    echo -e "  ${CYAN}4)${NC} xl       - Enterprise-scale (100K molecules, 12+3 workers)"
    echo -e "  ${CYAN}5)${NC} coconut  - Full COCONUT DB (1M molecules, 16+4 workers)"
    echo -e "  ${CYAN}q)${NC} quit\n"

    read -p "Enter choice [1-5, or q to quit]: " choice

    case $choice in
        1) PROFILE="small" ;;
        2|"") PROFILE="medium" ;;
        3) PROFILE="large" ;;
        4) PROFILE="xl" ;;
        5) PROFILE="coconut" ;;
        q|Q) echo -e "\n${YELLOW}Deployment cancelled.${NC}"; exit 0 ;;
        *)
            print_error "Invalid choice. Using default: medium"
            PROFILE="medium"
            ;;
    esac
}

# Function to validate profile
validate_profile() {
    local profile=$1
    for p in "${PROFILES[@]}"; do
        if [[ "$p" == "$profile" ]]; then
            return 0
        fi
    done
    return 1
}

# Function to parse YAML and export environment variables
parse_and_export_yaml() {
    local yaml_file=$1

    if [[ ! -f "$yaml_file" ]]; then
        print_error "Profile file not found: $yaml_file"
        exit 1
    fi

    print_success "Loading profile configuration..."

    # Parse YAML file (simple key: value format)
    while IFS=':' read -r key value; do
        # Skip comments and empty lines
        [[ "$key" =~ ^[[:space:]]*# ]] && continue
        [[ -z "$key" ]] && continue

        # Trim whitespace
        key=$(echo "$key" | xargs)
        value=$(echo "$value" | xargs)

        # Skip if no value
        [[ -z "$value" ]] && continue

        # Export as environment variable
        export "$key=$value"
        echo -e "  ${CYAN}${key}${NC}=${value}"
    done < "$yaml_file"
}

# Function to display configuration summary
show_config_summary() {
    echo -e "\n${BOLD}Configuration Summary:${NC}"
    echo -e "────────────────────────────────────"
    echo -e "  Max Batch Size:       ${CYAN}$(printf "%'d" $MAX_BATCH_SIZE)${NC} molecules"
    echo -e "  Max File Size:        ${CYAN}${MAX_FILE_SIZE_MB}${NC} MB"
    echo -e "  Celery Workers:       ${CYAN}${CELERY_WORKERS}${NC} (large jobs)"
    echo -e "  Priority Workers:     ${CYAN}${CELERY_WORKERS_PRIORITY:-2}${NC} (small jobs)"
    echo -e "  Gunicorn Workers:     ${CYAN}${GUNICORN_WORKERS}${NC}"
    echo -e "  Redis Memory:         ${CYAN}${REDIS_MAXMEMORY}${NC}"

    if [[ "${MAX_BATCH_SIZE}" -ge 50000 ]]; then
        echo -e "\n${YELLOW}⚠  Large batch processing enabled. Ensure adequate system resources.${NC}"
    fi
}

# Function to generate a secure random secret
generate_secret() {
    local length=${1:-64}
    openssl rand -hex $length 2>/dev/null || head -c $((length*2)) /dev/urandom | xxd -p | tr -d '\n'
}

# Function to generate secure secrets if they have placeholder values
generate_secrets() {
    local env_file=$1
    local secrets_generated=false

    echo -e "\n${BOLD}Checking security secrets...${NC}"

    # List of secrets that need to be generated
    # Format: VAR_NAME:LENGTH:PLACEHOLDER_PATTERN
    local secrets=(
        "SECRET_KEY:64:CHANGE_ME"
        "API_KEY_ADMIN_SECRET:32:CHANGE_ME"
        "CSRF_SECRET_KEY:32:CHANGE_ME"
        "POSTGRES_PASSWORD:24:CHANGE_ME"
        "GRAFANA_PASSWORD:16:CHANGE_ME"
    )

    for secret_spec in "${secrets[@]}"; do
        IFS=':' read -r var_name length placeholder <<< "$secret_spec"

        # Get current value
        current_value=$(grep "^${var_name}=" "$env_file" 2>/dev/null | cut -d'=' -f2-)

        # Check if it contains placeholder text or is empty
        if [[ -z "$current_value" ]] || [[ "$current_value" == *"$placeholder"* ]]; then
            local new_secret=$(generate_secret $length)

            if grep -q "^${var_name}=" "$env_file" 2>/dev/null; then
                # Update existing
                if [[ "$OSTYPE" == "darwin"* ]]; then
                    sed -i '' "s|^${var_name}=.*|${var_name}=${new_secret}|" "$env_file"
                else
                    sed -i "s|^${var_name}=.*|${var_name}=${new_secret}|" "$env_file"
                fi
            else
                # Append
                echo "${var_name}=${new_secret}" >> "$env_file"
            fi

            print_success "Generated secure ${var_name}"
            secrets_generated=true
        else
            echo -e "  ${GREEN}✓${NC} ${var_name} already configured"
        fi
    done

    if [[ "$secrets_generated" == true ]]; then
        echo -e "\n${YELLOW}⚠  New secrets generated. Keep your .env file secure!${NC}"
    fi
}

# Function to create/update .env file with profile settings
update_env_file() {
    local env_file="${SCRIPT_DIR}/.env"
    local env_example="${SCRIPT_DIR}/.env.example"

    # Create .env from example if it doesn't exist
    if [[ ! -f "$env_file" ]] && [[ -f "$env_example" ]]; then
        cp "$env_example" "$env_file"
        print_success "Created .env from .env.example"
    elif [[ ! -f "$env_file" ]]; then
        print_error ".env.example not found. Cannot create .env"
        exit 1
    fi

    # Generate secure secrets if needed
    generate_secrets "$env_file"

    # Update or append profile settings
    local vars=(
        "DEPLOYMENT_PROFILE"
        "MAX_BATCH_SIZE"
        "MAX_FILE_SIZE_MB"
        "CELERY_WORKERS"
        "CELERY_WORKERS_PRIORITY"
        "GUNICORN_WORKERS"
        "REDIS_MAXMEMORY"
        "CELERY_PRIORITY_MEMORY_LIMIT"
    )

    for var in "${vars[@]}"; do
        local value="${!var}"
        if [[ -n "$value" ]]; then
            if grep -q "^${var}=" "$env_file" 2>/dev/null; then
                # Update existing variable
                if [[ "$OSTYPE" == "darwin"* ]]; then
                    sed -i '' "s|^${var}=.*|${var}=${value}|" "$env_file"
                else
                    sed -i "s|^${var}=.*|${var}=${value}|" "$env_file"
                fi
            else
                # Append new variable
                echo "${var}=${value}" >> "$env_file"
            fi
        fi
    done

    print_success "Updated .env with profile settings"
}

# Function to run docker-compose
run_docker_compose() {
    echo -e "\n${BOLD}Starting Docker deployment...${NC}\n"

    # Check if docker-compose.prod.yml exists
    if [[ ! -f "${SCRIPT_DIR}/docker-compose.prod.yml" ]]; then
        print_error "docker-compose.prod.yml not found"
        exit 1
    fi

    # Build frontend if needed
    if [[ ! -d "${SCRIPT_DIR}/frontend-dist" ]]; then
        print_warning "frontend-dist not found. Building frontend..."
        cd "${SCRIPT_DIR}/frontend"
        npm run build
        mv dist ../frontend-dist
        cd "${SCRIPT_DIR}"
    fi

    # Pull latest images and start services
    docker-compose -f docker-compose.prod.yml pull
    docker-compose -f docker-compose.prod.yml up -d --build

    print_success "Deployment started successfully!"
    echo -e "\n${BOLD}Service Status:${NC}"
    docker-compose -f docker-compose.prod.yml ps
}

# Main execution
main() {
    print_header

    # Get profile from argument or interactive menu
    if [[ -n "$1" ]]; then
        PROFILE="$1"
        if ! validate_profile "$PROFILE"; then
            print_error "Invalid profile: $PROFILE"
            echo -e "Available profiles: ${PROFILES[*]}"
            exit 1
        fi
    else
        show_menu
    fi

    # Set profile name for export
    export DEPLOYMENT_PROFILE="$PROFILE"

    echo ""
    print_profile_info "$PROFILE"

    # Parse and export configuration
    parse_and_export_yaml "${CONFIG_DIR}/${PROFILE}.yml"

    # Show summary
    show_config_summary

    # Update .env file
    update_env_file

    # Confirmation prompt
    echo ""
    read -p "Proceed with deployment? [Y/n]: " confirm
    if [[ "$confirm" =~ ^[Nn]$ ]]; then
        echo -e "\n${YELLOW}Deployment cancelled.${NC}"
        exit 0
    fi

    # Run docker-compose
    run_docker_compose

    echo -e "\n${GREEN}${BOLD}Deployment complete!${NC}"
    echo -e "Access ChemAudit at: ${CYAN}http://localhost${NC}\n"
}

# Run main function
main "$@"
