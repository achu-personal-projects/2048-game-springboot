document.addEventListener('DOMContentLoaded', () => {
    const grid = document.querySelector('.grid');
    const scoreDisplay = document.getElementById('score');
    let score = 0;
    const gridSize = 4;
    let board = Array(gridSize).fill().map(() => Array(gridSize).fill(0));

    function initializeBoard() {
        board = Array(gridSize).fill().map(() => Array(gridSize).fill(0));
        score = 0;
        updateScore();
        addNewTile();
        addNewTile();
        renderBoard();
    }

    function addNewTile() {
        const emptyCells = [];
        for (let r = 0; r < gridSize; r++) {
            for (let c = 0; c < gridSize; c++) {
                if (board[r][c] === 0) emptyCells.push({r, c});
            }
        }
        if (emptyCells.length > 0) {
            const cell = emptyCells[Math.floor(Math.random() * emptyCells.length)];
            board[cell.r][cell.c] = Math.random() > 0.5 ? 2 : 4;
        }
    }

    function renderBoard() {
        grid.innerHTML = '';
        for (let r = 0; r < gridSize; r++) {
            for (let c = 0; c < gridSize; c++) {
                const cellValue = board[r][c];
                const cell = document.createElement('div');
                cell.className = 'grid-cell';
                
                if (cellValue !== 0) {
                    const tile = document.createElement('div');
                    tile.className = `tile tile-${cellValue}`;
                    tile.textContent = cellValue;
                    tile.style.top = `${r * 100 + 5}px`;
                    tile.style.left = `${c * 100 + 5}px`;
                    
                    // Different colors for different tile values
                    const colors = {
                        2: '#eee4da', 4: '#ede0c8', 8: '#f2b179',
                        16: '#f59563', 32: '#f67c5f', 64: '#f65e3b',
                        128: '#edcf72', 256: '#edcc61', 512: '#edc850',
                        1024: '#edc53f', 2048: '#edc22e'
                    };
                    tile.style.backgroundColor = colors[cellValue] || '#3c3a32';
                    tile.style.color = cellValue > 4 ? '#f9f6f2' : '#776e65';
                    
                    grid.appendChild(tile);
                }
            }
        }
    }

    function updateScore() {
        scoreDisplay.textContent = score;
    }

    function move(direction) {
        let moved = false;
        
        // Implement movement logic (left, right, up, down)
        // This is a simplified version - actual implementation would handle tile merging
        switch(direction) {
            case 'left':
                // Move tiles left
                for (let r = 0; r < gridSize; r++) {
                    for (let c = 1; c < gridSize; c++) {
                        if (board[r][c] !== 0) {
                            let newCol = c;
                            while (newCol > 0 && board[r][newCol-1] === 0) {
                                board[r][newCol-1] = board[r][newCol];
                                board[r][newCol] = 0;
                                newCol--;
                                moved = true;
                            }
                        }
                    }
                }
                break;
            // Similar implementations for right, up, down
        }
        
        if (moved) {
            addNewTile();
            renderBoard();
            checkGameStatus();
        }
    }

    function checkGameStatus() {
        // Check for win/lose conditions
    }

    // Initialize game
    initializeBoard();

    // Keyboard controls
    document.addEventListener('keydown', event => {
        switch(event.key) {
            case 'ArrowLeft': move('left'); break;
            case 'ArrowRight': move('right'); break;
            case 'ArrowUp': move('up'); break;
            case 'ArrowDown': move('down'); break;
        }
    });

    // Touch controls (mobile support)
    let touchStartX, touchStartY;
    grid.addEventListener('touchstart', event => {
        touchStartX = event.touches[0].clientX;
        touchStartY = event.touches[0].clientY;
    });
    
    grid.addEventListener('touchend', event => {
        const touchEndX = event.changedTouches[0].clientX;
        const touchEndY = event.changedTouches[0].clientY;
        
        const dx = touchEndX - touchStartX;
        const dy = touchEndY - touchStartY;
        
        if (Math.abs(dx) > Math.abs(dy)) {
            move(dx > 0 ? 'right' : 'left');
        } else {
            move(dy > 0 ? 'down' : 'up');
        }
    });
});
